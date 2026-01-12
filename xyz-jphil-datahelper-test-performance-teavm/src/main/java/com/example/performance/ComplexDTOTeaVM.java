package com.example.performance;

import lombok.Getter;
import lombok.Setter;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

import java.util.List;
import java.util.Map;

/**
 * Complex DTO for TeaVM performance testing.
 * Includes nested objects, lists, and maps to test serialization overhead.
 */
@DataHelper(propertyAnnotations = {JSProperty.class})
@Getter
@Setter
public class ComplexDTOTeaVM implements ComplexDTOTeaVM_I<ComplexDTOTeaVM> {
    // Simple fields
    String id;
    String name;
    int version;
    int status; // Using int instead of boolean (see PRP-13)

    // Nested object
    SimpleDTOTeaVM metadata;

    // List of simple types
    List<String> tags;

    // List of objects
    List<SimpleDTOTeaVM> relatedItems;

    // Map with simple values
    Map<String, String> properties;

    // Map with integer values
    Map<String, Integer> scores;
}
