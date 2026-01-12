package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSNumber;

/**
 * Test if we can use direct Java casting (JSNumber) instead of @JSBody
 * to convert Integer from @JSProperty to JSNumber
 */
public class JSNumberDirectCastTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Direct Java Cast (JSNumber) ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        testDirectCast(createWithNumber());

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        testDirectCast(createWithNull());

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        testDirectCast(createWithUndefined());

        // Test 4: JS object with zero
        System.out.println("\n--- Test 4: JS has zero (0) ---");
        testDirectCast(createWithZero());

        // Test 5: JS object with NaN
        System.out.println("\n--- Test 5: JS has NaN ---");
        testDirectCast(createWithNaN());

        // Test 6: JS object with negative number
        System.out.println("\n--- Test 6: JS has negative number (-42) ---");
        testDirectCast(createWithNegative());

        System.out.println("\n=== Test Complete ===");
    }

    private static void testDirectCast(TestInterface obj) {
        try {
            // Get as Integer
            Integer rawInt = obj.getAge();
            System.out.println("  Got as Integer: " + (rawInt == null ? "null" : "not null"));

            // Try DIRECT JAVA CASTING to JSNumber
            try {
                JSNumber jsNum = (JSNumber) (Object) rawInt;  // Cast via Object
                System.out.println("  Direct cast to JSNumber: " + (jsNum == null ? "null" : "success"));

                if (jsNum != null) {
                    // Check doubleValue first for NaN
                    double doubleVal = jsNum.doubleValue();
                    System.out.println("  doubleValue(): " + doubleVal);

                    if (Double.isNaN(doubleVal)) {
                        System.out.println("  Result: null (NaN detected)");
                    } else {
                        // Safe to convert to Integer
                        int intVal = jsNum.intValue();
                        System.out.println("  intValue(): " + intVal);

                        // Convert to proper Integer
                        Integer properInt = Integer.valueOf(intVal);
                        System.out.println("  Converted to Integer: " + properInt);
                        System.out.println("  Can use normally: " + (properInt + 10));
                    }
                }
            } catch (ClassCastException e) {
                System.out.println("  ERROR: ClassCastException - direct casting doesn't work");
                System.out.println("  Message: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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

/*
Actual output sample copy: 


test.js:334 === Testing Direct Java Cast (JSNumber) ===
test.js:334 
test.js:334 --- Test 1: JS has number value (28) ---
test.js:334   Got as Integer: not null
test.js:334   Direct cast to JSNumber: success
test.js:334   doubleValue(): 28.0
test.js:334   intValue(): 28
test.js:334   Converted to Integer: 28
test.js:334   Can use normally: 38
test.js:334 
test.js:334 --- Test 2: JS has explicit null ---
test.js:334   Got as Integer: null
test.js:334   Direct cast to JSNumber: null
test.js:334 
test.js:334 --- Test 3: JS has undefined (property missing) ---
test.js:334   Got as Integer: not null
test.js:334   Direct cast to JSNumber: success
test.js:334   doubleValue(): NaN
test.js:334   Result: null (NaN detected)
test.js:334 
test.js:334 --- Test 4: JS has zero (0) ---
test.js:334   Got as Integer: not null
test.js:334   Direct cast to JSNumber: success
test.js:334   doubleValue(): 0.0
test.js:334   intValue(): 0
test.js:334   Converted to Integer: 0
test.js:334   Can use normally: 10
test.js:334 
test.js:334 --- Test 5: JS has NaN ---
test.js:334   Got as Integer: not null
test.js:334   Direct cast to JSNumber: success
test.js:334   doubleValue(): NaN
test.js:334   Result: null (NaN detected)
test.js:334 
test.js:334 --- Test 6: JS has negative number (-42) ---
test.js:334   Got as Integer: not null
test.js:334   Direct cast to JSNumber: success
test.js:334   doubleValue(): -42.0
test.js:334   intValue(): -42
test.js:334   Converted to Integer: -42
test.js:334   Can use normally: -32
test.js:334 
test.js:334 === Test Complete ===


*/