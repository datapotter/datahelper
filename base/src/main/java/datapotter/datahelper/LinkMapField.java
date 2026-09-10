package datapotter.datahelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Type-safe field descriptor for a <b>keyed map of references</b> (MAP of LINK) to a DataHelper entity.
 *
 * <p>The map-of-links analogue of {@link LinkField}: an n:n keyed reference where the owner stores
 * a map of {@code key -> target RID}, not embedded copies (contrast {@link MapDataField}, which
 * embeds). The runtime value carrier is {@code datapotter.arcadedbhelper.LinkMap<K,TARGET>};
 * this symbol is the schema/metadata side and carries no ArcadeDB dependency.
 *
 * @param <PARENT> the entity declaring the reference map
 * @param <K>      the map key type (simple type)
 * @param <TARGET> the referenced (linked) value entity type
 */
public final class LinkMapField<PARENT extends DataHelper_I<PARENT>,
                                K,
                                TARGET extends DataHelper_I<TARGET>>
        implements Field_I<PARENT, Map<K, TARGET>> {

    private final String name;
    private final Class<K> keyType;
    private final Class<TARGET> valueType;
    private final List<Field_I<TARGET, ?>> valueFields;

    public LinkMapField(String name, Class<K> keyType, Class<TARGET> valueType,
                        List<Field_I<TARGET, ?>> valueFields) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
        this.valueFields = valueFields;
        this.__ = name();
    }

    public LinkMapField(String name, Class<K> keyType, Class<TARGET> valueType) {
        this(name, keyType, valueType, Collections.emptyList());
    }

    @Override public String name() { return name; }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Map<K, TARGET>> type() {
        return (Class<Map<K, TARGET>>) (Class<?>) Map.class;
    }

    public Class<K> keyType() { return keyType; }

    /** The linked value (target) entity type, e.g. {@code Account.class}. */
    public Class<TARGET> valueType() { return valueType; }

    /** The value type's FIELDS list, for zero-reflection schema constraint / projection. */
    public List<Field_I<TARGET, ?>> valueFields() { return valueFields; }

    public final String __;

    @Override public boolean validate(Object value) { return true; }

    public static <PARENT extends DataHelper_I<PARENT>, K, TARGET extends DataHelper_I<TARGET>>
    LinkMapField<PARENT, K, TARGET> of(String name, Class<K> keyType, Class<TARGET> valueType) {
        return new LinkMapField<>(name, keyType, valueType);
    }

    @Override
    public String toString() {
        return "LinkMapField[" + name + " -> Map<" + keyType.getSimpleName() +
               ", " + valueType.getSimpleName() + ">]";
    }
}
