package xyz.jphil.datahelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Type-safe field descriptor for Map fields with DataHelper values.
 *
 * <p>Extends the basic field concept to provide metadata about map value types
 * that are DataHelper entities, enabling compile-time verification and schema generation
 * without reflection.
 *
 * <p><b>Type Parameters:</b>
 * <ul>
 *   <li>{@code PARENT} - The parent DataHelper entity type (e.g., CompanyDTO)</li>
 *   <li>{@code K} - The map key type (e.g., String, Integer)</li>
 *   <li>{@code V} - The map value DataHelper entity type (e.g., EmployeeDTO)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Generated field constants
 * MapDataField&lt;CompanyDTO, String, EmployeeDTO&gt; $employees =
 *     new MapDataField&lt;&gt;("employees", String.class, EmployeeDTO.class, EmployeeDTO_A.FIELDS);
 *
 * // Schema registration can access value metadata:
 * Class&lt;?&gt; keyType = $employees.keyType();  // String.class
 * Class&lt;?&gt; valueType = $employees.valueType();  // EmployeeDTO.class
 * List&lt;Field_I&lt;?, ?&gt;&gt; valueFields = $employees.valueFields();  // EmployeeDTO_A.FIELDS
 * </pre>
 *
 * @param <PARENT> the parent DataHelper entity type
 * @param <K> the map key type
 * @param <V> the map value DataHelper entity type
 */
public final class MapDataField<PARENT extends DataHelper_I<PARENT>,
                                 K,
                                 V extends DataHelper_I<V>>
        implements Field_I<PARENT, Map<K, V>> {

    private final String name;
    private final Class<K> keyType;
    private final Class<V> valueType;
    private final List<Field_I<V, ?>> valueFields;

    /**
     * Constructor with key type, value type, and value fields reference.
     * Used by annotation processor to pass static FIELDS list.
     *
     * @param name the field name
     * @param keyType the map key type
     * @param valueType the map value type
     * @param valueFields the static FIELDS list from value type (e.g., EmployeeDTO_A.FIELDS)
     */
    public MapDataField(String name, Class<K> keyType, Class<V> valueType, List<Field_I<V, ?>> valueFields) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
        this.valueFields = valueFields;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates MapDataField without value fields reference.
     *
     * @param name the field name
     * @param keyType the map key type
     * @param valueType the map value type
     */
    public MapDataField(String name, Class<K> keyType, Class<V> valueType) {
        this(name, keyType, valueType, Collections.emptyList());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Map<K, V>> type() {
        // Return Map.class (type erasure means we can't return Map<K, V>.class)
        return (Class<Map<K, V>>) (Class<?>) Map.class;
    }

    /**
     * Get the key type for this map.
     *
     * @return the map key type (e.g., String.class)
     */
    public Class<K> keyType() {
        return keyType;
    }

    /**
     * Get the value type for this map.
     *
     * @return the map value type (e.g., EmployeeDTO.class)
     */
    public Class<V> valueType() {
        return valueType;
    }

    /**
     * Get the value type's field list.
     * Provides access to the value entity's FIELDS without reflection.
     *
     * @return the static FIELDS list from value type (e.g., EmployeeDTO_A.FIELDS)
     */
    public List<Field_I<V, ?>> valueFields() {
        return valueFields;
    }

    /**
     * Returns a string representation for debugging.
     *
     * @return a string like "MapDataField[employees: Map<String, EmployeeDTO>]"
     */
    @Override
    public String toString() {
        return "MapDataField[" + name + ": Map<" + keyType.getSimpleName() +
               ", " + valueType.getSimpleName() + ">]";
    }
}
