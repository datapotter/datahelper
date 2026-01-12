package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import xyz.jphil.teavm.dto.TeaVMMappers;

/**
 * Test that demonstrates fromValueProvider working with TeaVM JSMapLike.
 * This is the canonical approach recommended by the TeaVM maintainer.
 */
public class ValueProviderTest {

    public static void main(String[] args) {
        testFromValueProviderWithJSMapLike();
    }

    private static void testFromValueProviderWithJSMapLike() {
        // Test Case 1: Parse JSON and use fromValueProvider with JSMapLike
        String json1 = "{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"age\":28,\"email\":\"alice@example.com\"}";
        var personJson = (JSMapLike<JSObject>) parseJSON(json1);

        var person = new TeaVMTestDTO();
        // Use TeaVMMappers for clean, generic JavaScript to Java conversion
        person.fromTypedValueProvider(TeaVMMappers.fromJSMapLike(personJson));

        log("Test 1 - Full data:");
        log("  firstName: " + person.getFirstName()); // Should be "Alice"
        log("  lastName: " + person.getLastName());   // Should be "Smith"
        log("  age: " + person.getAge());             // Should be 28
        log("  email: " + person.getEmail());         // Should be "alice@example.com"

        // Test Case 2: Missing optional field (age is undefined)
        String json2 = "{\"firstName\":\"Bob\",\"lastName\":\"Jones\",\"email\":\"bob@example.com\"}";
        var person2Json = (JSMapLike<JSObject>) parseJSON(json2);

        var person2 = new TeaVMTestDTO();
        person2.fromTypedValueProvider(TeaVMMappers.fromJSMapLike(person2Json));

        log("Test 2 - Missing age field:");
        log("  firstName: " + person2.getFirstName()); // Should be "Bob"
        log("  age: " + person2.getAge());             // Should be null (undefined)

        // Test Case 3: Distinguish null from zero
        String json3 = "{\"firstName\":\"Baby\",\"lastName\":\"Doe\",\"age\":0,\"email\":\"baby@example.com\"}";
        var person3Json = (JSMapLike<JSObject>) parseJSON(json3);

        var person3 = new TeaVMTestDTO();
        person3.fromTypedValueProvider(TeaVMMappers.fromJSMapLike(person3Json));

        log("Test 3 - Age is zero:");
        log("  firstName: " + person3.getFirstName()); // Should be "Baby"
        log("  age: " + person3.getAge());             // Should be 0 (not null!)

        // Test Case 4: Explicit null
        String json4 = "{\"firstName\":\"Mystery\",\"lastName\":\"Person\",\"age\":null}";
        var person4Json = (JSMapLike<JSObject>) parseJSON(json4);

        var person4 = new TeaVMTestDTO();
        person4.fromTypedValueProvider(TeaVMMappers.fromJSMapLike(person4Json));

        log("Test 4 - Age is explicitly null:");
        log("  firstName: " + person4.getFirstName()); // Should be "Mystery"
        log("  age: " + person4.getAge());             // Should be null

        log("All tests completed!");
    }

    @JSBody(params = {"jsonString"}, script = "return JSON.parse(jsonString);")
    private static native JSObject parseJSON(String jsonString);

    @JSBody(params = {"message"}, script = "console.log(message);")
    private static native void log(String message);
}
