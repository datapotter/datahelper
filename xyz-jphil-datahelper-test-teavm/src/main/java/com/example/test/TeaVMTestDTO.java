package com.example.test;

import lombok.Data;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Test DTO for TeaVM JSProperty annotation support.
 * This demonstrates the use of propertyAnnotations and superInterfaces parameters
 * to add @JSProperty to all generated verbose getters and setters, and to make
 * the generated interface extend JSObject (required by TeaVM).
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class TeaVMTestDTO implements TeaVMTestDTO_T<TeaVMTestDTO> {
    String firstName;
    String lastName;
    Integer age; // Using Integer for nullable age (optional field)
    String email;
    String addedNewField;
}
