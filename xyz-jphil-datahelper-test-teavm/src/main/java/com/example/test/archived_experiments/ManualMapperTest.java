package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.json.JSON;

/**
 * Test manual JSON -> DTO mapping as recommended by TeaVM maintainer.
 * This is the CLEAN, CANONICAL way to handle nullable Integer in DTOs.
 *
 * Uses JSMapLike instead of @JSProperty to properly handle null/undefined/NaN.
 */
public class ManualMapperTest {

    public static void main(String[] args) {
        System.out.println("=== Testing Manual Mapper (Canonical Approach) ===\n");

        // Test 1: Complete object with all fields
        System.out.println("--- Test 1: Complete person (name + age 28) ---");
        testMapper("{\"name\":\"Alice\",\"age\":28}");

        // Test 2: Explicit null age
        System.out.println("\n--- Test 2: Explicit null age ---");
        testMapper("{\"name\":\"Bob\",\"age\":null}");

        // Test 3: Missing age field (undefined)
        System.out.println("\n--- Test 3: Missing age field ---");
        testMapper("{\"name\":\"Charlie\"}");

        // Test 4: Age is zero
        System.out.println("\n--- Test 4: Age is zero ---");
        testMapper("{\"name\":\"Baby\",\"age\":0}");

        // Test 5: Age is NaN
        System.out.println("\n--- Test 5: Age is NaN ---");
        testMapperWithNaN();

        // Test 6: Negative age
        System.out.println("\n--- Test 6: Negative age (-42) ---");
        testMapper("{\"name\":\"Test\",\"age\":-42}");

        System.out.println("\n=== Test Complete ===");
    }

    private static void testMapper(String jsonString) {
        try {
            // Parse JSON to JSMapLike (raw JS object)
            var personJson = (JSMapLike<JSObject>)JSON.parse(jsonString);
            
            // Create DTO and map manually
            PersonDTO person = new PersonDTO();

            // Map name (String - straightforward)
            JSObject nameObj = personJson.get("name");
            if (!JSObjects.isUndefined(nameObj)) {
                person.setName(((JSString) nameObj).stringValue());
            }

            // Map age (Integer - nullable, needs careful handling)
            JSObject ageObj = personJson.get("age");
            if (!JSObjects.isUndefined(ageObj)) {
                // Property exists, but might be null or NaN
                if (ageObj != null) {
                    JSNumber ageNum = (JSNumber) ageObj;
                    double doubleVal = ageNum.doubleValue();
                    if (!Double.isNaN(doubleVal)) {
                        person.setAge(Integer.valueOf(ageNum.intValue()));
                    }
                    // else: NaN -> leave as null
                }
                // else: explicit null -> leave as null
            }
            // else: undefined -> leave as null

            // Test the DTO
            System.out.println("  Name: " + person.getName());
            System.out.println("  Age: " + person.getAge());

            if (person.getAge() != null) {
                System.out.println("  Age + 10: " + (person.getAge() + 10));
                System.out.println("  toString: " + person.getAge().toString());
            }

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @JSBody(script = "var json = {name: 'NaNTest', age: NaN}; return json;")
    private static native JSMapLike<JSObject> createJsonWithNaN();

    private static void testMapperWithNaN() {
        try {
            JSMapLike<JSObject> personJson = createJsonWithNaN();

            PersonDTO person = new PersonDTO();

            JSObject nameObj = personJson.get("name");
            if (!JSObjects.isUndefined(nameObj)) {
                person.setName(((JSString) nameObj).stringValue());
            }

            JSObject ageObj = personJson.get("age");
            if (!JSObjects.isUndefined(ageObj)) {
                if (ageObj != null) {
                    JSNumber ageNum = (JSNumber) ageObj;
                    double doubleVal = ageNum.doubleValue();
                    if (!Double.isNaN(doubleVal)) {
                        person.setAge(Integer.valueOf(ageNum.intValue()));
                    }
                }
            }

            System.out.println("  Name: " + person.getName());
            System.out.println("  Age: " + person.getAge());

        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plain Java POJO - NO TeaVM annotations, works in both JVM and TeaVM!
     * This is the clean approach for shared DTOs.
     */
    public static class PersonDTO {
        private String name;
        private Integer age;  // Nullable Integer - can distinguish null from 0

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
