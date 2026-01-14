package xyz.jphil.arcadedb.datahelper.test;

import xyz.jphil.arcadedb.datahelper.ArcadeData;
import xyz.jphil.arcadedb.datahelper.TypeDef;
import java.util.List;

/**
 * Test DTO with nested types - demonstrates all nested type scenarios:
 * 1. Single embedded object (AddressDTO)
 * 2. List of embedded objects (List<PhoneNumberDTO>)
 * 3. Simple primitives
 */
@ArcadeData
public final class EmployeeDTO extends EmployeeDTO_A {

    String employeeId;
    String firstName;
    String lastName;
    String email;
    Integer age;

    // Nested embedded object
    AddressDTO homeAddress;
    AddressDTO workAddress;

    // List of embedded objects
    List<PhoneNumberDTO> phoneNumbers;

    // Simple list
    List<String> skills;

    static {
        __.chars();
    }

    /**
     * Schema definition with unique constraint on employeeId.
     */
    public static final TypeDef<EmployeeDTO> TYPEDEF =
            schemaBuilder()
                .unique($employeeId)
                .lsmIndex($email)
                .__();
}
