package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Test to determine what value primitive int gets when JS has null or undefined
 */
public class IntPrimitiveNullTest {

    public static void main(String[] args) {
        System.out.println("=== Testing primitive int with null/undefined ===\n");

        // Test 1: JS object with number value (28)
        System.out.println("--- Test 1: JS has number value (28) ---");
        TestInterface obj1 = createWithNumber();
        int age1 = obj1.getAge();
        System.out.println("  int value: " + age1);
        System.out.println("  equals 28: " + (age1 == 28));
        System.out.println("  equals 0: " + (age1 == 0));

        // Test 2: JS object with explicit null
        System.out.println("\n--- Test 2: JS has explicit null ---");
        TestInterface obj2 = createWithNull();
        int age2 = obj2.getAge();
        System.out.println("  int value: " + age2);
        System.out.println("  equals 0: " + (age2 == 0));
        System.out.println("  equals -1: " + (age2 == -1));

        // Test 3: JS object with undefined (property not set)
        System.out.println("\n--- Test 3: JS has undefined (property missing) ---");
        TestInterface obj3 = createWithUndefined();
        int age3 = obj3.getAge();
        System.out.println("  int value: " + age3);
        System.out.println("  equals 0: " + (age3 == 0));
        System.out.println("  equals -1: " + (age3 == -1));

        // Test 4: JS object with NaN
        System.out.println("\n--- Test 4: JS has NaN ---");
        TestInterface obj4 = createWithNaN();
        int age4 = obj4.getAge();
        System.out.println("  int value: " + age4);
        System.out.println("  equals 0: " + (age4 == 0));

        System.out.println("\n=== Test Complete ===");
    }

    interface TestInterface extends JSObject {
        @JSProperty("age")
        int getAge();
    }

    @JSBody(script = "return {age: 28};")
    private static native TestInterface createWithNumber();

    @JSBody(script = "return {age: null};")
    private static native TestInterface createWithNull();

    @JSBody(script = "return {};")
    private static native TestInterface createWithUndefined();

    @JSBody(script = "return {age: NaN};")
    private static native TestInterface createWithNaN();
}
