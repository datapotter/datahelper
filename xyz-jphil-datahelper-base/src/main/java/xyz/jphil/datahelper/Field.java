package xyz.jphil.datahelper;

/**
 * Type-safe field descriptor for simple (non-DataHelper) fields.
 *
 * <p>Represents fields with primitive types, Strings, or other non-DataHelper objects.
 * For nested DataHelper fields, use {@link DataField} which supports type-safe chaining.
 *
 * <p><b>Type Parameters:</b>
 * <ul>
 *   <li>{@code E} - The DataHelper entity type (e.g., TestPersonDTO)</li>
 *   <li>{@code T} - The field value type (e.g., String, Integer)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Generated field constants
 * Field&lt;TestPersonDTO, String&gt; $email = new Field&lt;&gt;("email", String.class);
 * Field&lt;TestPersonDTO, Integer&gt; $age = new Field&lt;&gt;("age", Integer.class);
 *
 * // Type-safe DSL usage
 * import static TestPersonDTO_I.*;
 *
 * person.in(db)
 *     .whereEq($email, person.email())  // Type-checked
 *     .fields($name, $age)              // Refactor-safe
 *     .upsert();
 *
 * // Compile errors prevent mistakes:
 * .whereEq($age, "thirty")  // ✗ Compile error: String ≠ Integer
 * .fields($companyName)     // ✗ Compile error: Wrong entity type
 * </pre>
 *
 * @param <E> the DataHelper entity type this field belongs to
 * @param <T> the value type of this field
 * @param name the field name (used internally for property access)
 * @param type the field's value type class (used for validation and reflection)
 */
public final class Field<E extends DataHelper_I<E>, T>
        implements Field_I<E, T> {
    
    private final String name;
    private final Class<T> type;

    public Field(String name, Class<T> type) {
        this.name = name; this.type = type;
    }

    @Override public String name() { return name; }
    @Override public Class<T> type() { return type; }

    
    
    
    /**
     * Factory method to create a Field instance.
     *
     * @param name the field name
     * @param type the field type
     * @param <E> the entity type
     * @param <T> the field value type
     * @return a new Field instance
     */
    public static <E extends DataHelper_I<E>, T> Field<E, T> of(String name, Class<T> type) {
        return new Field<>(name, type);
    }

    /**
     * Returns a string representation for debugging.
     *
     * @return a string like "Field[email: String]"
     */
    @Override
    public String toString() {
        return "Field[" + name + ": " + type.getSimpleName() + "]";
    }
    
    public final String __ = name();
}
