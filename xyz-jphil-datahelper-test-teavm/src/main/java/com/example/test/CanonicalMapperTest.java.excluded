package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.json.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * Test using annotation processor generated DTO with toMap/fromMap helpers.
 * This is the CANONICAL way recommended by TeaVM maintainer.
 *
 * Uses JSMapLike for parsing, then converts to Java Map, then uses fromMap().
 * Keeps Integer nullable and distinguishes null/undefined/0/NaN properly.
 */
public class CanonicalMapperTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Canonical Mapper with Generated DTO ===\n");

        // Test 1: Complete object
        System.out.println("--- Test 1: Complete person (name + nullableAge 28) ---");
        testMapper("{\"name\":\"Alice\",\"nullableAge\":28,\"primitiveAge\":30}");

        // Test 2: Explicit null age
        System.out.println("\n--- Test 2: Explicit null nullableAge ---");
        testMapper("{\"name\":\"Bob\",\"nullableAge\":null,\"primitiveAge\":25}");

        // Test 3: Missing age field (undefined)
        System.out.println("\n--- Test 3: Missing nullableAge field ---");
        testMapper("{\"name\":\"Charlie\",\"primitiveAge\":40}");

        // Test 4: Age is zero
        System.out.println("\n--- Test 4: nullableAge is zero ---");
        testMapper("{\"name\":\"Baby\",\"nullableAge\":0,\"primitiveAge\":0}");

        // Test 5: Age is NaN
        System.out.println("\n--- Test 5: nullableAge is NaN ---");
        testMapperWithNaN();

        // Test 6: Negative age
        System.out.println("\n--- Test 6: Negative nullableAge (-42) ---");
        testMapper("{\"name\":\"Test\",\"nullableAge\":-42,\"primitiveAge\":50}");

        System.out.println("\n=== Test Complete ===");
    }

    private static void testMapper(String jsonString) {
        try {
            // Step 1: Parse JSON to JSMapLike (raw JS object)
            JSObject parsed = JSON.parse(jsonString);
            JSMapLike<JSObject> jsonObj = (JSMapLike<JSObject>) parsed;

            // Step 2: Convert JSMapLike to Java Map using canonical approach
            Map<String, Object> javaMap = jsMapToJavaMap(jsonObj);

            // Step 3: Create DTO and use fromMap (from annotation processor)
            NullableNumberTestDTO dto = new NullableNumberTestDTO();
            dto.fromMap(javaMap);

            // Test the DTO
            System.out.println("  Name: " + dto.getName());
            System.out.println("  NullableAge: " + dto.getNullableAge());
            System.out.println("  PrimitiveAge: " + dto.getPrimitiveAge());

            if (dto.getNullableAge() != null) {
                System.out.println("  NullableAge + 10: " + (dto.getNullableAge() + 10));
                System.out.println("  toString: " + dto.getNullableAge().toString());
            }

            // Test toMap (reverse direction)
            Map<String, Object> backToMap = dto.toMap();
            System.out.println("  toMap keys: " + backToMap.keySet());

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @JSBody(script = "return {name: 'NaNTest', nullableAge: NaN, primitiveAge: 99};")
    private static native JSMapLike<JSObject> createJsonWithNaN();

    private static void testMapperWithNaN() {
        try {
            JSMapLike<JSObject> jsonObj = createJsonWithNaN();
            Map<String, Object> javaMap = jsMapToJavaMap(jsonObj);

            NullableNumberTestDTO dto = new NullableNumberTestDTO();
            dto.fromMap(javaMap);

            System.out.println("  Name: " + dto.getName());
            System.out.println("  NullableAge: " + dto.getNullableAge());
            System.out.println("  PrimitiveAge: " + dto.getPrimitiveAge());

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Canonical way to convert JSMapLike to Java Map.
     * Handles Integer/Double with proper null/undefined/NaN checks.
     */
    private static Map<String, Object> jsMapToJavaMap(JSMapLike<JSObject> jsMap) {
        Map<String, Object> javaMap = new HashMap<>();
        
        // For each field in the DTO
        for (String field : NullableNumberTestDTO_I.FIELDS) {
            JSObject value = jsMap.get(field);

            if (!JSObjects.isUndefined(value)) {
                // Field exists in JSON

                if (value == null) {
                    // Explicit null
                    javaMap.put(field, null);
                } else if (value instanceof JSString) {
                    // String field
                    javaMap.put(field, ((JSString) value).stringValue());
                } else if (value instanceof JSNumber) {
                    // Number field - check if it's NaN
                    JSNumber numValue = (JSNumber) value;
                    double doubleVal = numValue.doubleValue();

                    if (Double.isNaN(doubleVal)) {
                        // NaN -> treat as null for Integer fields
                        // (could also skip adding to map)
                        javaMap.put(field, null);
                    } else {
                        // Valid number - determine if Integer or Double needed
                        if (field.equals(NullableNumberTestDTO_I.$primitiveAge)) {
                            // Primitive int field
                            javaMap.put(field, numValue.intValue());
                        } else if (field.equals(NullableNumberTestDTO_I.$nullableAge)) {
                            // Nullable Integer field
                            javaMap.put(field, Integer.valueOf(numValue.intValue()));
                        } else if (field.equals(NullableNumberTestDTO_I.$nullableScore)) {
                            // Nullable Double field
                            javaMap.put(field, Double.valueOf(doubleVal));
                        }
                    }
                }
                // Add other types as needed (Boolean, arrays, nested objects, etc.)
            }
            // If undefined, don't add to map (leave DTO field as default/null)
        }

        return javaMap;
    }
}
