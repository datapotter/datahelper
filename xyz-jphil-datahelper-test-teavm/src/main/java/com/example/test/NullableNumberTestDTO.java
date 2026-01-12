package com.example.test;

import lombok.Data;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Test DTO to explore nullable number handling in TeaVM.
 *
 * PROBLEM: We need nullable numbers (Integer, not int) to represent optional values,
 * but TeaVM doesn't automatically convert wrapper types with @JSProperty.
 *
 * This test explores different approaches to solve this.
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class NullableNumberTestDTO implements NullableNumberTestDTO_T<NullableNumberTestDTO> {
    String name;

    // Approach 1: Using Integer directly (will this work?)
    Integer nullableAge;

    // Approach 2: Using Double for better JS compatibility
    Double nullableScore;

    // For comparison: non-nullable primitive
    int primitiveAge;
}
