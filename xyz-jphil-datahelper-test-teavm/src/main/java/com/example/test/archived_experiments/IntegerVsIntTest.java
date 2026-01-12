package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Focused test to understand exactly what TeaVM returns for int vs Integer
 * with different JavaScript values: number, null, undefined
 */
public class IntegerVsIntTest {

    public static void main(String[] args) {
        System.out.println("=== Testing int vs Integer with different JS values ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        testWithIntGetter(createWithNumber_int());
        testWithIntegerGetter(createWithNumber_Integer());

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        testWithIntGetter(createWithNull_int());
        testWithIntegerGetter(createWithNull_Integer());

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        testWithIntGetter(createWithUndefined_int());
        testWithIntegerGetter(createWithUndefined_Integer());

        System.out.println("\n=== Test Complete ===");
    }

    // Test with int getter
    private static void testWithIntGetter(TestInterface_Int obj) {
        System.out.println("  Using: int getAge()");
        try {
            int age = obj.getAge();
            System.out.println("    ✓ Got int: " + age);
            System.out.println("    ✓ Direct print: " + obj.getAge());
            System.out.println("    ✓ String concat: Age is " + obj.getAge());
        } catch (Exception e) {
            System.out.println("    ✗ Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // Test with Integer getter
    private static void testWithIntegerGetter(TestInterface_Integer obj) {
        System.out.println("  Using: Integer getAge()");
        try {
            Integer age = obj.getAge();
            System.out.println("    Step 1: Got Integer object: " + (age == null ? "null" : "not null"));

            if (age != null) {
                System.out.println("    Step 2: Value check: age != null = true");
                try {
                    // Try direct print
                    System.out.println("    Step 3: Try direct print: " + age);
                } catch (Exception e) {
                    System.out.println("    ✗ Step 3 failed (direct print): " + e.getClass().getSimpleName());
                }

                try {
                    // Try intValue()
                    int intVal = age.intValue();
                    System.out.println("    Step 4: intValue() = " + intVal);
                } catch (Exception e) {
                    System.out.println("    ✗ Step 4 failed (intValue): " + e.getClass().getSimpleName());
                }

                try {
                    // Try toString()
                    String str = age.toString();
                    System.out.println("    Step 5: toString() = " + str);
                } catch (Exception e) {
                    System.out.println("    ✗ Step 5 failed (toString): " + e.getClass().getSimpleName());
                }

                try {
                    // Try string concatenation
                    String concat = "Age: " + age;
                    System.out.println("    Step 6: String concat = " + concat);
                } catch (Exception e) {
                    System.out.println("    ✗ Step 6 failed (concat): " + e.getClass().getSimpleName());
                }
            } else {
                System.out.println("    ✓ age is null");
            }
        } catch (Exception e) {
            System.out.println("    ✗ Exception getting Integer: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // Interface with int getter
    interface TestInterface_Int extends JSObject {
        @JSProperty("age")
        int getAge();
    }

    // Interface with Integer getter
    interface TestInterface_Integer extends JSObject {
        @JSProperty("age")
        Integer getAge();
    }

    // Create JS object with number value - int version
    @JSBody(script = "return {age: 28};")
    private static native TestInterface_Int createWithNumber_int();

    // Create JS object with number value - Integer version
    @JSBody(script = "return {age: 28};")
    private static native TestInterface_Integer createWithNumber_Integer();

    // Create JS object with explicit null - int version
    @JSBody(script = "return {age: null};")
    private static native TestInterface_Int createWithNull_int();

    // Create JS object with explicit null - Integer version
    @JSBody(script = "return {age: null};")
    private static native TestInterface_Integer createWithNull_Integer();

    // Create JS object without age property (undefined) - int version
    @JSBody(script = "return {};")
    private static native TestInterface_Int createWithUndefined_int();

    // Create JS object without age property (undefined) - Integer version
    @JSBody(script = "return {};")
    private static native TestInterface_Integer createWithUndefined_Integer();
}
