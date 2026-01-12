package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Demo to test nullable number handling in TeaVM.
 *
 * Goal: Find the correct way to handle Integer/Double fields from JavaScript
 * so they can be displayed in console without errors.
 */
public class NullableNumberDemo {

    public static void main(String[] args) {
        System.out.println("=== Testing Nullable Numbers in TeaVM ===\n");

        // Test 1: Create JS object with all values present
        System.out.println("Test 1: All values present");
        NullableNumberTestDTO_T<?> person1 = createPerson("Alice", 25, 95.5, 30);
        testDisplayPerson(person1, "person1");

        // Test 2: Create JS object with null age
        System.out.println("\nTest 2: Null age value");
        NullableNumberTestDTO_T<?> person2 = createPersonWithNullAge("Bob", 85.0, 35);
        testDisplayPerson(person2, "person2");

        // Test 3: Create JS object with undefined age
        System.out.println("\nTest 3: Undefined age value");
        NullableNumberTestDTO_T<?> person3 = createPersonWithUndefinedAge("Charlie", 75.5, 40);
        testDisplayPerson(person3, "person3");

        // Test 4: Create JS object with all nullables missing
        System.out.println("\nTest 4: All nullable values missing");
        NullableNumberTestDTO_T<?> person4 = createPersonMinimal("Diana", 45);
        testDisplayPerson(person4, "person4");
    }

    /**
     * Test different approaches to display a person with potentially null numbers.
     */
    private static void testDisplayPerson(NullableNumberTestDTO_T<?> person, String label) {
        System.out.println("  " + label + ".getName(): " + person.getName());
        System.out.println("  " + label + ".getPrimitiveAge(): " + person.getPrimitiveAge());

        // APPROACH 1: Direct call (likely to fail with wrapper types)
        System.out.println("  --- Approach 1: Direct call ---");
        try {
            System.out.println("  " + label + ".getNullableAge() direct: " + person.getNullableAge());
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // APPROACH 2: Store in variable, check null, then convert
        System.out.println("  --- Approach 2: Variable + null check + intValue() ---");
        try {
            Integer age = person.getNullableAge();
            String ageStr = (age != null) ? String.valueOf(age.intValue()) : "null";
            System.out.println("  " + label + ".getNullableAge() with intValue(): " + ageStr);
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // APPROACH 3: String concatenation to force conversion
        System.out.println("  --- Approach 3: String concatenation ---");
        try {
            Integer age = person.getNullableAge();
            String ageStr = (age != null) ? ("" + age) : "null";
            System.out.println("  " + label + ".getNullableAge() with concat: " + ageStr);
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // APPROACH 4: Using JSBody to access raw JS value
        System.out.println("  --- Approach 4: Direct JS access ---");
        try {
            String ageStr = getAgeAsString(person);
            System.out.println("  " + label + " age via JS: " + ageStr);
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // APPROACH 5: Check if property exists in JS first
        System.out.println("  --- Approach 5: Check JS property existence ---");
        try {
            if (hasNullableAge(person)) {
                String ageStr = getAgeAsString(person);
                System.out.println("  " + label + " has age: " + ageStr);
            } else {
                System.out.println("  " + label + " has no age property");
            }
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Test Double field similarly
        System.out.println("  --- Testing Double field ---");
        try {
            Double score = person.getNullableScore();
            String scoreStr = (score != null) ? String.valueOf(score) : "null";
            System.out.println("  " + label + ".getNullableScore(): " + scoreStr);
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Create a person with all fields.
     */
    @JSBody(params = {"name", "nullableAge", "nullableScore", "primitiveAge"},
            script = "return {name: name, nullableAge: nullableAge, nullableScore: nullableScore, primitiveAge: primitiveAge};")
    private static native NullableNumberTestDTO_T<?> createPerson(String name, Integer nullableAge,
                                                                    Double nullableScore, int primitiveAge);

    /**
     * Create a person with explicit null age.
     */
    @JSBody(params = {"name", "nullableScore", "primitiveAge"},
            script = "return {name: name, nullableAge: null, nullableScore: nullableScore, primitiveAge: primitiveAge};")
    private static native NullableNumberTestDTO_T<?> createPersonWithNullAge(String name, Double nullableScore,
                                                                               int primitiveAge);

    /**
     * Create a person with undefined age (property not set).
     */
    @JSBody(params = {"name", "nullableScore", "primitiveAge"},
            script = "return {name: name, nullableScore: nullableScore, primitiveAge: primitiveAge};")
    private static native NullableNumberTestDTO_T<?> createPersonWithUndefinedAge(String name, Double nullableScore,
                                                                                    int primitiveAge);

    /**
     * Create a person with minimal fields (only required).
     */
    @JSBody(params = {"name", "primitiveAge"},
            script = "return {name: name, primitiveAge: primitiveAge};")
    private static native NullableNumberTestDTO_T<?> createPersonMinimal(String name, int primitiveAge);

    /**
     * Access the age directly from JavaScript as a string.
     */
    @JSBody(params = {"obj"},
            script = "return obj.nullableAge === undefined ? 'undefined' : " +
                     "(obj.nullableAge === null ? 'null' : String(obj.nullableAge));")
    private static native String getAgeAsString(JSObject obj);

    /**
     * Check if the nullableAge property exists and is not null/undefined.
     */
    @JSBody(params = {"obj"},
            script = "return obj.nullableAge !== undefined && obj.nullableAge !== null;")
    private static native boolean hasNullableAge(JSObject obj);
}
