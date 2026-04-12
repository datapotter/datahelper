package xyz.jphil.datahelper;

/**
 * Type-safe field descriptor for nested DataHelper fields.
 *
 * <p>Extends the basic field concept with support for type-safe nested field access
 * using the {@link #__(Field_I)} method. This allows compile-time verification that
 * nested fields actually belong to the nested entity type.
 *
 * <p><b>Type Parameters:</b>
 * <ul>
 *   <li>{@code PARENT} - The parent DataHelper entity type (e.g., PersonDTO)</li>
 *   <li>{@code NESTED} - The nested DataHelper entity type (e.g., AddressDTO)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Generated field constants
 * DataField&lt;PersonDTO, AddressDTO&gt; $address =
 *     new DataField&lt;&gt;("address", AddressDTO.class);
 *
 * // In AddressDTO_I:
 * Field&lt;AddressDTO, String&gt; $city = new Field&lt;&gt;("city", String.class);
 * Field&lt;AddressDTO, String&gt; $zipCode = new Field&lt;&gt;("zipCode", String.class);
 *
 * // Type-safe nested field access:
 * import static PersonDTO_I.*;
 * import static AddressDTO_I.*;
 *
 * person.in(db)
 *     .whereEq($address.__($city), "NYC")      // ✓ Type-safe!
 *     .whereEq($address.__($zipCode), "10001")  // ✓ Type-safe!
 *     .upsert();
 *
 * // Compile error - $companyName is from CompanyDTO, not AddressDTO:
 * person.in(db)
 *     .whereEq($address.__($companyName), "...")  // ✗ Won't compile!
 * </pre>
 *
 * <p><b>String Fallback:</b>
 * For deeply nested or dynamic paths, string-based API is still available:
 * <pre>
 * person.in(db)
 *     .whereEq("address.location.country.code", "US")  // Complex path
 *     .upsert();
 * </pre>
 *
 * @param <PARENT> the parent DataHelper entity type
 * @param <NESTED> the nested DataHelper entity type
 * @param name the field name (used internally for property access)
 * @param type the nested entity's class (used for validation and reflection)
 */
public final class DataField<PARENT extends DataHelper_I<PARENT>,
                        NESTED extends DataHelper_I<NESTED>>
        implements Field_I<PARENT, NESTED> {

    private final String name;
    private final Class<NESTED> type;
    private final java.util.List<Field_I<NESTED, ?>> nestedFields;

    /**
     * Constructor with nested fields reference.
     * Used by annotation processor to pass static FIELDS list.
     *
     * @param name the field name
     * @param type the nested entity type
     * @param nestedFields the static FIELDS list from nested type (e.g., AddressDTO_A.FIELDS)
     */
    public DataField(String name, Class<NESTED> type, java.util.List<Field_I<NESTED, ?>> nestedFields) {
        this.name = name;
        this.type = type;
        this.nestedFields = nestedFields;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates DataField without nested fields reference.
     *
     * @param name the field name
     * @param type the nested entity type
     */
    public DataField(String name, Class<NESTED> type) {
        this(name, type, java.util.Collections.emptyList());
    }

    @Override public String name() { return name; }
    @Override public Class<NESTED> type() { return type; }

    /**
     * Get the nested type's field list.
     * Provides access to the nested entity's FIELDS without reflection.
     *
     * @return the static FIELDS list from nested type (e.g., AddressDTO_A.FIELDS)
     */
    public java.util.List<Field_I<NESTED, ?>> nestedFields() {
        return nestedFields;
    }
    
    public final String __ = name();
                        
    /**
     * Factory method to create a DataField instance.
     *
     * @param name the field name
     * @param type the nested entity type
     * @param <PARENT> the parent entity type
     * @param <NESTED> the nested entity type
     * @return a new DataField instance
     */
    public static <PARENT extends DataHelper_I<PARENT>,
                   NESTED extends DataHelper_I<NESTED>>
    DataField<PARENT, NESTED> of(String name, Class<NESTED> type) {
        return new DataField<>(name, type);
    }

    /**
     * Chain with a simple (non-DataHelper) field to create a nested path.
     *
     * <p>Use this when the nested field is a simple type (String, Integer, etc.)
     * and you want to terminate the chain.
     *
     * <p><b>Example:</b>
     * <pre>
     * // PersonDTO has AddressDTO field, AddressDTO has String city
     * DataField&lt;PersonDTO, AddressDTO&gt; $address = ...;
     * Field&lt;AddressDTO, String&gt; $city = ...;
     *
     * // Creates "address.city" path (type-safe!)
     * Field&lt;PersonDTO, String&gt; addressCity = $address.__($city);
     *
     * // Use in query:
     * person.in(db)
     *     .whereEq($address.__($city), "NYC")
     *     .upsert();
     * </pre>
     *
     * <p><b>Type Safety:</b> Only accepts Field (not DataField) from NESTED entity.
     *
     * @param subField the simple nested field (must be Field&lt;NESTED, SUBTYPE&gt;)
     * @param <SUBTYPE> the type of the nested field's value
     * @return a Field representing the path "this.name.subField.name"
     */
    public <SUBTYPE> Field<PARENT, SUBTYPE> __(Field<NESTED, SUBTYPE> subField) {
        return new Field<>(this.name + "." + subField.name(), subField.type());
    }

    /**
     * Chain with another DataField to allow multi-level nesting.
     *
     * <p>Use this when the nested field is itself a DataHelper type,
     * allowing you to chain further with __() or ___().
     *
     * <p><b>Example:</b>
     * <pre>
     * // PersonDTO has AddressDTO field, AddressDTO has LocationDTO field
     * DataField&lt;PersonDTO, AddressDTO&gt; $address = ...;
     * DataField&lt;AddressDTO, LocationDTO&gt; $location = ...;
     * Field&lt;LocationDTO, String&gt; $country = ...;
     *
     * // Multi-level chaining:
     * Field&lt;PersonDTO, String&gt; path = $address.___($location).__($country);
     * // Creates "address.location.country"
     *
     * // Use in query:
     * person.in(db)
     *     .whereEq($address.___($location).__($country), "USA")
     *     .upsert();
     * </pre>
     *
     * <p><b>Type Safety:</b> Only accepts DataField from NESTED entity,
     * ensuring correct nesting hierarchy.
     *
     * @param subField the nested DataHelper field (must be DataField&lt;NESTED, DEEPER&gt;)
     * @param <DEEPER> the deeper nested DataHelper entity type
     * @return a DataField allowing further chaining
     */
    public <DEEPER extends DataHelper_I<DEEPER>>
    DataField<PARENT, DEEPER> ___(DataField<NESTED, DEEPER> subField) {
        return new DataField<>(this.name + "." + subField.name(), subField.type());
    }

    /**
     * Returns a string representation for debugging.
     *
     * @return a string like "DataField[address: AddressDTO]"
     */
    @Override
    public String toString() {
        return "DataField[" + name + ": " + type.getSimpleName() + "]";
    }
}
