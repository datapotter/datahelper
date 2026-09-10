package datapotter.datahelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The object-to-map serializer: the mirror of {@link MapReads}, and the write half of the plain-map
 * round trip that makes a DataHelper storable by anything that can hold a map.
 *
 * <p>Grounded on the readable {@link DataHelper_IR}, so it converts an immutable {@code _R} record
 * projection as readily as a mutable instance. Recursion is deep through nested blocks, lists of
 * them and maps of them; enum values cross the boundary through
 * {@link HasUuid#storageValue(Object)}, the same conversion every backend already uses on write.</p>
 *
 * <p>{@code null} fields are omitted rather than stored as null keys, which keeps a partial object
 * a partial map &mdash; and keeps the round trip through {@link MapReads} an update rather than an
 * erasure. Insertion order follows {@link DataHelper_IR#fieldNames()}, so the map is stable and
 * readable.</p>
 */
public final class MapWrites {

    private MapWrites() {}

    /** Deep name&rarr;value map of {@code self}; an empty map for {@code null}. */
    public static Map<String, Object> toMap(DataHelper_IR<?> self) {
        var out = new LinkedHashMap<String, Object>();
        if (self == null) return out;
        for (var fieldName : self.fieldNames()) {
            var value = self.getPropertyByName(fieldName);
            if (value != null) out.put(fieldName, convert(self, fieldName, value));
        }
        return out;
    }

    private static Object convert(DataHelper_IR<?> self, String fieldName, Object value) {
        if (self.isNestedObjectField(fieldName) && value instanceof DataHelper_IR<?> nested) {
            return toMap(nested);
        }
        if (self.isListField(fieldName) && value instanceof List<?> source) {
            var out = new ArrayList<Object>(source.size());
            for (var item : source) out.add(element(item));
            return out;
        }
        if (self.isMapField(fieldName) && value instanceof Map<?, ?> source) {
            var out = new LinkedHashMap<Object, Object>();
            for (var entry : source.entrySet()) out.put(entry.getKey(), element(entry.getValue()));
            return out;
        }
        return HasUuid.storageValue(value);
    }

    private static Object element(Object item) {
        return item instanceof DataHelper_IR<?> nested ? toMap(nested) : HasUuid.storageValue(item);
    }
}
