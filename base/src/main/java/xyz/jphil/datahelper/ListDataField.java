package xyz.jphil.datahelper;

import java.util.Collections;
import java.util.List;

/**
 * Type-safe field descriptor for List fields containing DataHelper elements.
 *
 * <p>Extends the basic field concept to provide metadata about list element types
 * that are DataHelper entities, enabling compile-time verification and schema generation
 * without reflection.
 *
 * <p><b>Type Parameters:</b>
 * <ul>
 *   <li>{@code PARENT} - The parent DataHelper entity type (e.g., EmployeeDTO)</li>
 *   <li>{@code ELEMENT} - The list element DataHelper entity type (e.g., PhoneNumberDTO)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Generated field constants
 * ListDataField&lt;EmployeeDTO, PhoneNumberDTO&gt; $phoneNumbers =
 *     new ListDataField&lt;&gt;("phoneNumbers", PhoneNumberDTO.class, PhoneNumberDTO_A.FIELDS);
 *
 * // Schema registration can access element metadata:
 * Class&lt;?&gt; elementType = $phoneNumbers.elementType();  // PhoneNumberDTO.class
 * List&lt;Field_I&lt;?, ?&gt;&gt; elementFields = $phoneNumbers.elementFields();  // PhoneNumberDTO_A.FIELDS
 * </pre>
 *
 * @param <PARENT> the parent DataHelper entity type
 * @param <ELEMENT> the list element DataHelper entity type
 */
public final class ListDataField<PARENT extends DataHelper_I<PARENT>,
                                  ELEMENT extends DataHelper_I<ELEMENT>>
        implements Field_I<PARENT, List<ELEMENT>> {

    private final String name;
    private final Class<ELEMENT> elementType;
    private final List<Field_I<ELEMENT, ?>> elementFields;

    /**
     * Constructor with element type and fields reference.
     * Used by annotation processor to pass static FIELDS list.
     *
     * @param name the field name
     * @param elementType the list element type
     * @param elementFields the static FIELDS list from element type (e.g., PhoneNumberDTO_A.FIELDS)
     */
    public ListDataField(String name, Class<ELEMENT> elementType, List<Field_I<ELEMENT, ?>> elementFields) {
        this.name = name;
        this.elementType = elementType;
        this.elementFields = elementFields;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates ListDataField without element fields reference.
     *
     * @param name the field name
     * @param elementType the list element type
     */
    public ListDataField(String name, Class<ELEMENT> elementType) {
        this(name, elementType, Collections.emptyList());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<ELEMENT>> type() {
        // Return List.class (type erasure means we can't return List<ELEMENT>.class)
        return (Class<List<ELEMENT>>) (Class<?>) List.class;
    }

    /**
     * Get the element type for this list.
     *
     * @return the list element type (e.g., PhoneNumberDTO.class)
     */
    public Class<ELEMENT> elementType() {
        return elementType;
    }

    /**
     * Get the element type's field list.
     * Provides access to the element entity's FIELDS without reflection.
     *
     * @return the static FIELDS list from element type (e.g., PhoneNumberDTO_A.FIELDS)
     */
    public List<Field_I<ELEMENT, ?>> elementFields() {
        return elementFields;
    }

    /**
     * Returns a string representation for debugging.
     *
     * @return a string like "ListDataField[phoneNumbers: List<PhoneNumberDTO>]"
     */
    @Override
    public String toString() {
        return "ListDataField[" + name + ": List<" + elementType.getSimpleName() + ">]";
    }
}
