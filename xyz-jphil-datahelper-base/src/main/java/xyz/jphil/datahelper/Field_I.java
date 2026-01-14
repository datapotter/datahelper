package xyz.jphil.datahelper;

/**
 * Sealed interface for type-safe field descriptors in DataHelper entities.
 *
 * <p>This is a sealed type allowing four implementations:
 * <ul>
 *   <li>{@link Field} - for simple fields (String, Integer, etc.)</li>
 *   <li>{@link DataField} - for nested DataHelper fields with type-safe chaining</li>
 *   <li>{@link ListDataField} - for list fields containing DataHelper elements</li>
 *   <li>{@link MapDataField} - for map fields with DataHelper values</li>
 * </ul>
 *
 * <p><b>Design Benefits:</b>
 * <ul>
 *   <li>Type safety: Compiler enforces correct field types</li>
 *   <li>Flexibility: API accepts Field_I for maximum flexibility</li>
 *   <li>Clarity: Generated code explicitly shows Field vs DataField</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Generated code uses specific types:
 * Field&lt;PersonDTO, String&gt; $name = new Field&lt;&gt;("name", String.class);
 * DataField&lt;PersonDTO, AddressDTO&gt; $address = new DataField&lt;&gt;("address", AddressDTO.class);
 *
 * // API accepts the interface:
 * public ArcadeDocUpdate&lt;E&gt; whereEq(Field_I&lt;E, ?&gt; field, Object value) { ... }
 *
 * // Usage with type-safe nested access:
 * person.in(db)
 *     .whereEq($name, "John")                    // Simple field
 *     .whereEq($address.__($city), "NYC")        // Nested field (type-safe!)
 *     .upsert();
 * </pre>
 *
 * @param <E> the DataHelper entity type this field belongs to
 * @param <T> the value type of this field
 */
public sealed interface Field_I<E extends DataHelper_I<E>, T>
        permits Field, DataField, ListDataField, MapDataField {

    /**
     * Get the field name.
     *
     * @return the field name (e.g., "email", "address.city")
     */
    String name();

    /**
     * Get the field's value type.
     *
     * @return the Class object representing the field type
     */
    Class<T> type();

    /**
     * Validate if a value is compatible with this field's type.
     *
     * <p>Performs runtime type checking to ensure type safety during
     * dynamic operations (e.g., deserialization, reflection-based access).
     *
     * @param value the value to validate
     * @return true if the value is null or an instance of this field's type
     */
    default boolean validate(Object value) {
        if (value == null) {
            return true;
        }
        return type().isInstance(value);
    }
}
