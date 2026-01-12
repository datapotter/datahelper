package com.example.test;

import lombok.Data;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Person DTO with nested objects (Address and Company)
 * Tests complex nested object structures in both JVM and TeaVM
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class PersonDTO implements PersonDTO_I<PersonDTO> {
    String firstName;
    String lastName;
    Integer age;
    String email;
    AddressDTO homeAddress;    // Nested object
    AddressDTO workAddress;    // Another nested object (can be null)
    CompanyDTO company;        // Nested object with further nesting
}
