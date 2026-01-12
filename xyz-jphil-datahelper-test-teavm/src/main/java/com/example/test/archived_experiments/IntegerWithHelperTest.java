package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Test to verify if the @JSBody helper method works for Integer
 */
public class IntegerWithHelperTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Integer with @JSBody helper ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        TestInterface obj1 = createWithNumber();
        Integer age1 = obj1.getAge();
        System.out.println("  Got Integer: " + (age1 == null ? "null" : "not null"));
        if (age1 != null) {
            try {
                String ageStr = integerToString(age1);
                System.out.println("  integerToString() result: " + ageStr);
                System.out.println("  Can use in string concat: Age is " + ageStr);
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        TestInterface obj2 = createWithNull();
        Integer age2 = obj2.getAge();
        System.out.println("  Got Integer: " + (age2 == null ? "null" : "not null"));
        if (age2 != null) {
            try {
                String ageStr = integerToString(age2);
                System.out.println("  integerToString() result: " + ageStr);
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName());
            }
        } else {
            System.out.println("  age is null - correct!");
        }

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        TestInterface obj3 = createWithUndefined();
        Integer age3 = obj3.getAge();
        System.out.println("  Got Integer: " + (age3 == null ? "null" : "not null"));
        if (age3 != null) {
            try {
                String ageStr = integerToString(age3);
                System.out.println("  integerToString() result: " + ageStr);
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName());
            }
        } else {
            System.out.println("  age is null - correct!");
        }

        // Test 4: JS object with zero
        System.out.println("\n--- Test 4: JS has zero (0) ---");
        TestInterface obj4 = createWithZero();
        Integer age4 = obj4.getAge();
        System.out.println("  Got Integer: " + (age4 == null ? "null" : "not null"));
        if (age4 != null) {
            try {
                String ageStr = integerToString(age4);
                System.out.println("  integerToString() result: " + ageStr);
                System.out.println("  Successfully distinguished zero from null!");
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName());
            }
        } else {
            System.out.println("  age is null - WRONG! Should be 0");
        }

        // Test 5: JS object with NaN
        System.out.println("\n--- Test 5: JS has NaN ---");
        TestInterface obj5 = createWithNaN();
        Integer age5 = obj5.getAge();
        System.out.println("  Got Integer: " + (age5 == null ? "null" : "not null"));
        if (age5 != null) {
            try {
                String ageStr = integerToString(age5);
                System.out.println("  integerToString() result: " + ageStr);
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getClass().getSimpleName());
            }
        } else {
            System.out.println("  age is null");
        }

        System.out.println("\n=== Test Complete ===");
    }

    interface TestInterface extends JSObject {
        @JSProperty("age")
        Integer getAge();
    }

    // Helper method to safely convert Integer to String
    @JSBody(params = {"num"}, script = "return num == null ? null : String(num);")
    private static native String integerToString(Integer num);

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
}
