package datapotter.datahelper;

/**
 * Sealed interface for type-safe field descriptors in DataHelper entities.
 *
 * <p>This is a sealed type allowing these implementations:
 * <ul>
 *   <li>{@link Field} - for simple fields (String, Integer, etc.)</li>
 *   <li>{@link DataField} - for nested DataHelper fields with type-safe chaining</li>
 *   <li>{@link ListDataField} - for list fields containing DataHelper elements</li>
 *   <li>{@link MapDataField} - for map fields with DataHelper values</li>
 *   <li>{@link LinkField} - for a reference (LINK) to another DataHelper entity</li>
 *   <li>{@link LinkListField} - for a list of references (LIST of LINK)</li>
 *   <li>{@link LinkMapField} - for a keyed map of references (MAP of LINK)</li>
 *   <li>{@link EnumField} - for an enum field, carrying its own storage resolver</li>
 *   <li>{@link EnumListField} - for a list of enums, resolving element-wise</li>
 * </ul>
 *
 * <p><b>On adding to this list:</b> every consumer dispatches over these with an {@code instanceof}
 * chain that falls through to plain-field handling, never an exhaustive {@code switch}, so a new
 * descriptor is additive. A new shape that must NOT be treated as a plain scalar has to be added to
 * those chains deliberately.</p>
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
        permits Field, DataField, ListDataField, MapDataField,
                LinkField, LinkListField, LinkMapField,
                EnumField, EnumListField {

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

    /**
     * The field's stable identity (PRP-28 phase 2), or {@code null} if it carries none.
     *
     * <p>{@code @P} is {@code RetentionPolicy.SOURCE} (in {@code datahelper/annotations}), so it does
     * not exist at runtime and there is no reflective path back to it — the id has to be carried by
     * the generated field-symbol instance itself, which is what this method reaches. A {@code default}
     * returning {@code null} rather than an abstract method: every existing implementation of this
     * sealed interface, hand-written or already generated, keeps compiling untouched, and {@code null}
     * for "no identity declared" is the house idiom already used for an unresolved enum id.
     *
     * <p>Not named {@code id()}: ArcadeDB already means the record id by that name, and
     * {@code $outcome.id()} sitting a line away from {@code doc.getIdentity()} would be confusing. Not
     * {@code uid()}: collides in spirit with {@link HasUuid#uuid()} from phase 1. This name says what
     * the thing is — an identity that survives a rename — and is backend-neutral, which matters because
     * the annotation lives in {@code datahelper}, not in any one backend.
     */
    default String stableId() { return null; }
}
