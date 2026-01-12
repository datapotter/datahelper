package com.example.performance;

import lombok.Getter;
import lombok.Setter;
import xyz.jphil.datahelper.DataHelper;

import java.util.List;
import java.util.Map;

/**
 * Complex DTO for comprehensive performance testing.
 * Includes nested objects, lists, and maps to test serialization overhead.
 */
@DataHelper
@Getter
@Setter
public class ComplexDTO implements ComplexDTO_I<ComplexDTO> {
    // Simple fields
    String id;
    String name;
    int version;
    int status; // Using int instead of boolean to avoid Lombok isXxx() vs getXxx() conflict (see PRP-13)

    // Nested object
    SimpleDTO metadata;

    // List of simple types
    List<String> tags;

    // List of objects
    List<SimpleDTO> relatedItems;

    // Map with simple values
    Map<String, String> properties;

    // Map with integer values
    Map<String, Integer> scores;
}
