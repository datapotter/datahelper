package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Final solution: Proper conversion from JSObject Integer to real Java Integer
 * Two-step process: JSObject → int primitive → Java Integer
 */
public class IntegerFinalSolutionTest {

    public static void main(String[] args) {
        System.out.println("=== Testing FINAL Integer conversion solution ===\n");

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
            // Use the proper conversion helper
            Integer properAge = getProperInteger(obj);

            System.out.println("  Proper Integer is null: " + (properAge == null));

            if (properAge != null) {
                // Now test ALL Java Integer operations
                System.out.println("  Direct print: " + properAge);
                System.out.println("  intValue(): " + properAge.intValue());
                System.out.println("  toString(): " + properAge.toString());
                System.out.println("  String concat: Age is " + properAge);
                System.out.println("  Arithmetic: " + (properAge + 10));
                System.out.println("  Comparison: " + (properAge > 0 ? "positive" : "non-positive"));
                System.out.println("  SUCCESS! All operations work!");
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
     * FINAL SOLUTION: Properly converts JSObject Integer to real Java Integer.
     * Uses two-step process:
     * 1. JavaScript: null/undefined check and convert to int primitive
     * 2. Java: Integer.valueOf(int) to create proper Java Integer object
     */
    private static Integer getProperInteger(TestInterface obj) {
        Integer rawValue = obj.getAge();
        if (isNullOrUndefined(rawValue)) {
            return null;
        }
        // Convert to primitive int, then to proper Integer
        int primitiveValue = toInt(rawValue);
        // Check if the conversion resulted in NaN (becomes 0)
        if (isNaN(rawValue)) {
            return null;
        }
        return Integer.valueOf(primitiveValue);
    }

    /**
     * Checks if the JS value is null or undefined
     */
    @JSBody(params = {"val"}, script = "return val === null || val === undefined;")
    private static native boolean isNullOrUndefined(Integer val);

    /**
     * Checks if the JS value is NaN
     */
    @JSBody(params = {"val"}, script = "return typeof val === 'number' && isNaN(val);")
    private static native boolean isNaN(Integer val);

    /**
     * Converts JS number to int primitive (handles conversion)
     */
    @JSBody(params = {"val"}, script = "return val | 0;")
    private static native int toInt(Integer val);

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
