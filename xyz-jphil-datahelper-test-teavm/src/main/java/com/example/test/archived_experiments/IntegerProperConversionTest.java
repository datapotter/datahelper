package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Test proper conversion from JSObject Integer to real Java Integer
 */
public class IntegerProperConversionTest {

    public static void main(String[] args) {
        System.out.println("=== Testing proper Integer conversion ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        testConversion(createWithNumber(), "28");

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        testConversion(createWithNull(), "null");

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        testConversion(createWithUndefined(), "undefined");

        // Test 4: JS object with zero
        System.out.println("\n--- Test 4: JS has zero (0) ---");
        testConversion(createWithZero(), "0");

        // Test 5: JS object with NaN
        System.out.println("\n--- Test 5: JS has NaN ---");
        testConversion(createWithNaN(), "NaN");

        // Test 6: JS object with negative number
        System.out.println("\n--- Test 6: JS has negative number (-42) ---");
        testConversion(createWithNegative(), "-42");

        System.out.println("\n=== Test Complete ===");
    }

    private static void testConversion(TestInterface obj, String expected) {
        try {
            // Get the raw JSObject Integer
            Integer rawAge = obj.getAge();
            System.out.println("  Raw Integer is null: " + (rawAge == null));

            // Convert to proper Integer
            Integer properAge = toProperInteger(rawAge);
            System.out.println("  Proper Integer is null: " + (properAge == null));

            if (properAge != null) {
                // Now test all Java Integer operations
                System.out.println("  Value: " + properAge);
                System.out.println("  intValue(): " + properAge.intValue());
                System.out.println("  toString(): " + properAge.toString());
                System.out.println("  String concat: Age is " + properAge);
                System.out.println("  Arithmetic: " + (properAge + 10));
                System.out.println("  Comparison: " + (properAge > 0 ? "positive" : "non-positive"));
            } else {
                System.out.println("  Correctly identified as null!");
            }
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    interface TestInterface extends JSObject {
        @JSProperty("age")
        Integer getAge();
    }

    /**
     * Properly converts a JSObject Integer to a real Java Integer.
     * Handles null, undefined, NaN, and valid numbers.
     */
    @JSBody(params = {"jsNum"}, script =
        "if (jsNum === null || jsNum === undefined) return null; " +
        "var n = Number(jsNum); " +
        "if (isNaN(n)) return null; " +
        "return n;"
    )
    private static native Integer toProperInteger(Integer jsNum);

    @JSBody(script = "return {age: 28};")
    private static native TestInterface createWithNumber();

    @JSBody(script = "return {age: null};")
    private static native TestInterface createWithNull();

    @JSBody(script = "return {};")
    private static native TestInterface createWithUndefined();

    @JSBody(script = "return {age: 0};")
    private static native TestInterface createWithZero();

    @JSBody(script = "return {age: NaN};")
    private static native TestInterface createWithNaN();

    @JSBody(script = "return {age: -42};")
    private static native TestInterface createWithNegative();
}
