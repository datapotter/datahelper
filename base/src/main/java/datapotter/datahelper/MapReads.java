package datapotter.datahelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The map-to-object deserializer: reads a name&rarr;value map into a {@link DataHelper_I} using
 * only the generated accessor contract, recursing through nested DataHelper blocks, lists of them
 * and maps of them.
 *
 * <p>It lives in {@code base}, below every backend, because nothing in it is backend-specific: the
 * members it uses ({@code fieldNames}, {@code getPropertyType}, {@code setPropertyByName}, the
 * {@code create*} factories and the {@code is*Field} predicates) are generated for every
 * DataHelper, whether or not a database is anywhere in the picture. Whatever a particular store
 * adds on top of a plain map arrives through {@link MapReadContext}.</p>
 *
 * <p>Placement matters for correctness, not just tidiness. A deserializer that lives on one
 * backend's trait interface can only reach types that implement that trait, so an embedded block
 * &mdash; a plain {@code @Data} type, deliberately carrying no database dependency &mdash; is
 * unreachable from it. That was the defect this class exists to remove: such a block was written
 * correctly and read back as {@code null}, and a {@code List} of them read back holding raw maps.</p>
 *
 * <p>Nothing is ever stored under a field whose declared type it does not fit. Where a value has
 * the shape of a record but the field cannot accept one, the read fails here, naming the type and
 * the field, rather than putting a foreign object into a typed field and deferring the failure to
 * whichever unrelated code first calls an accessor.</p>
 */
public final class MapReads {

    private MapReads() {}

    /** Read {@code map} into {@code self} and return {@code self}, for fluent chaining. */
    public static <E extends DataHelper_I<E>> E read(E self, Map<String, Object> map, MapReadContext ctx) {
        readInto(self, map, ctx);
        return self;
    }

    /**
     * Read {@code map} into {@code self}. Absent keys and {@code null} values leave the field
     * untouched, so a partial map (a projection) is a partial update rather than an erasure.
     *
     * <p>Untyped counterpart to {@link #read} for a nested instance, whose static type is only
     * {@code DataHelper_I<?>} and so cannot satisfy the self-referential bound.</p>
     */
    public static void readInto(DataHelper_I<?> self, Map<String, Object> map, MapReadContext ctx) {
        if (self == null || map == null) return;
        for (var fieldName : self.fieldNames()) {
            if (map.containsKey(fieldName)) readField(self, fieldName, map.get(fieldName), ctx);
        }
    }

    /**
     * Read one already-extracted value into one field. The entry point for a driver that iterates
     * its own record rather than a map &mdash; a backend reading fields straight off a native
     * document has no reason to materialize an intermediate map first.
     */
    public static void readField(DataHelper_I<?> self, String fieldName, Object value, MapReadContext ctx) {
        if (value == null) return;
        var fieldType = self.getPropertyType(fieldName);
        if (fieldType == null) return;
        if (ctx.readTraitField(self, fieldName, value)) return;

        if (self.isEnumField(fieldName)) {
            readEnum(self, fieldName, value, fieldType);
        } else if (self.isEnumListField(fieldName) && value instanceof List<?> source) {
            readEnumList(self, fieldName, source);
        } else if (self.isNestedObjectField(fieldName)) {
            readNested(self, fieldName, value, ctx);
        } else if (self.isListField(fieldName) && value instanceof List<?> source) {
            readList(self, fieldName, source, ctx);
        } else if (self.isMapField(fieldName) && value instanceof Map<?, ?> source) {
            readMap(self, fieldName, source, ctx);
        } else {
            self.setPropertyByName(fieldName, DataHelper_I.convertType(value, fieldType));
        }
    }

    /**
     * An enum arrives as its stored string (a uuid or a name, per the field's declaration) and is
     * resolved by the generated, reflection-free lookup; an actual constant passes through. Never
     * throws on an unrecognised string: that means the row was written by newer code than this
     * build, not that it is corrupt, so the field is left null.
     */
    private static void readEnum(DataHelper_I<?> self, String fieldName, Object value, Class<?> fieldType) {
        if (value instanceof String stored) {
            self.setPropertyByName(fieldName, self.resolveEnumFromStorage(fieldName, stored));
        } else if (fieldType.isInstance(value)) {
            self.setPropertyByName(fieldName, value);
        }
    }

    /**
     * A {@code List<E>} of enums arrives as a list of stored strings, each resolved by the same
     * generated lookup a bare enum field uses. An element that resolves to nothing is
     * <em>dropped</em>, not stored as null: the field's declared type admits no way to keep the
     * unrecognised string, and a null sitting in a {@code List<E>} would travel — through
     * {@code List.copyOf} in the record projection, and into whatever iterates the list — turning
     * "written by a newer build" into a failure somewhere with no connection to this field. So the
     * list a caller receives holds exactly the constants this build can name.
     */
    private static void readEnumList(DataHelper_I<?> self, String fieldName, List<?> source) {
        var target = new ArrayList<Object>(source.size());
        for (var item : source) {
            var resolved = item instanceof String stored ? self.resolveEnumFromStorage(fieldName, stored) : item;
            if (resolved != null) target.add(resolved);
        }
        self.setPropertyByName(fieldName, target);
    }

    private static void readNested(DataHelper_I<?> self, String fieldName, Object value, MapReadContext ctx) {
        var fields = ctx.asFieldMap(value);
        if (fields == null) throw notRecord(self, fieldName, "value", value);
        var nested = self.createNestedObject(fieldName);
        if (nested == null) throw noFactory(self, fieldName, "createNestedObject");
        readInto(nested, fields, ctx);
        self.setPropertyByName(fieldName, nested);
    }

    /**
     * Elements that are records become instances of the declared element type; scalars are stored
     * as they came. A record-shaped element in a list that has no element factory is the one case
     * that throws &mdash; it is the shape the old code silently added raw, turning a clear failure
     * into a {@code ClassCastException} in unrelated code much later.
     */
    private static void readList(DataHelper_I<?> self, String fieldName, List<?> source, MapReadContext ctx) {
        var target = new ArrayList<Object>(source.size());
        for (var item : source) {
            var fields = item == null ? null : ctx.asFieldMap(item);
            if (fields == null) {
                target.add(item);
                continue;
            }
            var element = self.createListElement(fieldName);
            if (element == null) throw noFactory(self, fieldName, "createListElement");
            readInto(element, fields, ctx);
            target.add(element);
        }
        self.setPropertyByName(fieldName, target);
    }

    @SuppressWarnings("unchecked")
    private static void readMap(DataHelper_I<?> self, String fieldName, Map<?, ?> source, MapReadContext ctx) {
        var target = (Map<Object, Object>) self.createMapInstance(fieldName);
        if (target == null) throw noFactory(self, fieldName, "createMapInstance");
        var keyType = self.getMapKeyType(fieldName);
        var valueType = self.getMapValueType(fieldName);
        var valuesAreDataHelper = self.isMapValueDataHelper(fieldName);

        for (var entry : source.entrySet()) {
            var key = DataHelper_I.convertType(entry.getKey(), keyType);
            var value = entry.getValue();
            if (!valuesAreDataHelper || value == null) {
                target.put(key, DataHelper_I.convertType(value, valueType));
                continue;
            }
            var fields = ctx.asFieldMap(value);
            if (fields == null) throw notRecord(self, fieldName, "map value", value);
            var element = self.createMapValueElement(fieldName);
            if (element == null) throw noFactory(self, fieldName, "createMapValueElement");
            readInto(element, fields, ctx);
            target.put(key, element);
        }
        self.setPropertyByName(fieldName, target);
    }

    private static IllegalStateException noFactory(DataHelper_IR<?> self, String fieldName, String factory) {
        return new IllegalStateException(field(self, fieldName)
                + ": the stored value has named fields, but " + factory + "() returned null, so no"
                + " instance of the declared type can be built for it. Regenerate this type, or"
                + " override " + factory + "() if it is a hand-written DataHelper_I.");
    }

    private static IllegalStateException notRecord(DataHelper_IR<?> self, String fieldName,
                                                   String role, Object value) {
        return new IllegalStateException(field(self, fieldName) + ": the stored " + role + " is a "
                + value.getClass().getName() + ", which has no named fields to read into the"
                + " declared DataHelper type. Storing it as it stands would put a foreign object"
                + " into a typed field, so the read fails here instead of at the first accessor.");
    }

    private static String field(DataHelper_IR<?> self, String fieldName) {
        return (self.dataClass() == null ? "?" : self.dataClass().getName()) + "." + fieldName;
    }
}
