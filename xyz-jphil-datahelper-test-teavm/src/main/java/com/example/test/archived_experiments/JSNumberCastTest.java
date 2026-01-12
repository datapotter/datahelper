package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSNumber;

/**
 * Test to see if we can cast Integer from @JSProperty to JSNumber
 * Based on TeaVM developer's suggestion to use JSNumber
 */
public class JSNumberCastTest {

    public static void main(String[] args) {
        System.out.println("=== Testing JSNumber cast approach ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        testCast(createWithNumber());

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        testCast(createWithNull());

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        testCast(createWithUndefined());

        // Test 4: JS object with zero
        System.out.println("\n--- Test 4: JS has zero (0) ---");
        testCast(createWithZero());

        // Test 5: JS object with NaN
        System.out.println("\n--- Test 5: JS has NaN ---");
        testCast(createWithNaN());

        // Test 6: JS object with negative number
        System.out.println("\n--- Test 6: JS has negative number (-42) ---");
        testCast(createWithNegative());

        System.out.println("\n=== Test Complete ===");
    }

    private static void testCast(TestInterface obj) {
        try {
            // Get as Integer
            Integer rawInt = obj.getAge();
            System.out.println("  Got as Integer: " + (rawInt == null ? "null" : "not null"));

            // Try to cast to JSNumber
            try {
                JSNumber jsNum = castToJSNumber(rawInt);
                System.out.println("  Cast to JSNumber: " + (jsNum == null ? "null" : "success"));

                if (jsNum != null) {
                    // Try JSNumber methods
                    int intVal = jsNum.intValue();
                    double doubleVal = jsNum.doubleValue();
                    System.out.println("  intValue(): " + intVal);
                    System.out.println("  doubleValue(): " + doubleVal);

                    // Convert to proper Integer
                    Integer properInt = Integer.valueOf(intVal);
                    System.out.println("  Converted to Integer: " + properInt);
                    System.out.println("  Can use normally: " + (properInt + 10));
                }
            } catch (Exception e) {
                System.out.println("  ERROR casting to JSNumber: " + e.getClass().getSimpleName());
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
     * Try to cast Integer to JSNumber
     */
    @JSBody(params = {"val"}, script = "return val;")
    private static native JSNumber castToJSNumber(Integer val);

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
