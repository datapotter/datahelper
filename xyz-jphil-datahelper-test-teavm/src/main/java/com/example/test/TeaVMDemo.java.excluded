package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * TeaVM Demo showing native JavaScript interop using our generated DTO interface.
 * This demonstrates how the generated interface with @JSProperty and JSObject extension
 * allows seamless Java-JavaScript interop.
 */
public class TeaVMDemo {

    public static void main(String[] args) {
        System.out.println("TeaVM Demo - Native JS Interop with Generated DTO Interface");

        // EXPERIMENT 1: Try to create the Java class directly in TeaVM
        System.out.println("\n=== EXPERIMENT 1: Creating Java class in TeaVM ===");
        try {
            TeaVMTestDTO javaObject = new TeaVMTestDTO();
            javaObject.setFirstName("JavaObject");
            javaObject.setLastName("Test");
            javaObject.setAge(99);
            javaObject.setEmail("java@test.com");
            System.out.println("✓ Java object created successfully!");
            System.out.println("  Name: " + javaObject.getFirstName() + " " + javaObject.getLastName());
            Integer javaAge = javaObject.getAge();
            System.out.println("  Age: " + (javaAge != null ? javaAge : "null"));

            // Test 1: Can we use it as the interface?
            System.out.println("\n  Test 1: Using Java object as interface");
            TeaVMTestDTO_I<?> asInterface = javaObject; // upcast to interface
            System.out.println("  Via interface getName: " + asInterface.getFirstName());
            System.out.println("  Fluent API: " + asInterface.firstName("Fluent").firstName());
            System.out.println("  FIELDS constant: " + asInterface.FIELDS);

            // Test 2: Can we pass it to a JS function?
            System.out.println("\n  Test 2: Passing Java object to JS function");
            String result = testPassToJS(javaObject);
            System.out.println("  Result from JS: " + result);

            // Test 3: Check if interface symbols are accessible
            System.out.println("\n  Test 3: Symbol constants from interface");
            System.out.println("  $firstName = " + TeaVMTestDTO_I.$firstName);
            System.out.println("  $age = " + TeaVMTestDTO_I.$age);
            System.out.println("  Via object: javaObject.$firstName would not compile");
            System.out.println("  (Symbols are in interface, not in the Java object instance)");

        } catch (Exception e) {
            System.out.println("✗ Failed to create Java object: " + e.getMessage());
            e.printStackTrace();
        }

        // EXPERIMENT 2: Create a JavaScript object using our generated interface
        System.out.println("\n=== EXPERIMENT 2: Creating JS object via interface ===");
        TeaVMTestDTO_I<?> person = createPersonFromJS("John", "Doe", 30, "john.doe@example.com", "Developer");

        // Read properties using Java getters (mapped to JS properties via @JSProperty)
        String fullName = person.getFirstName() + " " + person.getLastName();
        System.out.println("Person: " + fullName);
        Integer personAge = person.getAge();
        System.out.println("Age: " + (personAge != null ? personAge : "null"));
        System.out.println("Email: " + person.getEmail());
        System.out.println("Field: " + person.getAddedNewField());

        // Modify using Java setters (mapped to JS properties via @JSProperty)
        person.setAge(31);
        person.setEmail("john.updated@example.com");

        System.out.println("\nAfter update:");
        Integer updatedAge = person.getAge();
        System.out.println("Age: " + (updatedAge != null ? updatedAge : "null"));
        System.out.println("Email: " + person.getEmail());

        // Use fluent API
        person.firstName("Jane")
              .lastName("Smith")
              .age(25);

        System.out.println("\nAfter fluent update:");
        System.out.println("Name: " + person.getFirstName() + " " + person.getLastName());
        Integer fluentAge = person.getAge();
        System.out.println("Age: " + (fluentAge != null ? fluentAge : "null"));

        // Access the FIELDS constant
        System.out.println("\nAvailable fields: " + person.FIELDS);

        // Convert to Map and back
        var map = person.toMap();
        System.out.println("\nConverted to Map: " + map);

        // Create an array of persons in JavaScript
        JSArray<TeaVMTestDTO_I<?>> people = createPeopleArray();
        System.out.println("\nCreated " + people.getLength() + " people in JS array");

        for (int i = 0; i < people.getLength(); i++) {
            TeaVMTestDTO_I<?> p = people.get(i);
            String firstName = p.getFirstName();
            String lastName = p.getLastName();

            // Test 1: Direct access to $value field
            Integer age = p.getAge();
            int ageValue = getValueFromInteger(age);
            System.out.println("  - " + firstName + " " + lastName + " (age via $value: " + ageValue + ")");
        }

        // Display in DOM
        displayInDOM(person);

        // EXPERIMENT 3: Plain Java object without JSProperty/JSObject
        System.out.println("\n=== EXPERIMENT 3: Plain object (no @JSProperty, no JSObject) ===");
        try {
            PlainTestDTO plainObj = new PlainTestDTO();
            plainObj.setFirstName("Plain");
            plainObj.setLastName("Object");
            plainObj.setAge(42);
            plainObj.setEmail("plain@test.com");

            System.out.println("✓ Plain object created successfully!");
            System.out.println("  Name: " + plainObj.getFirstName() + " " + plainObj.getLastName());
            Integer plainAge = plainObj.getAge();
            System.out.println("  Age: " + (plainAge != null ? plainAge : "null"));

            // Test passing to JS
            System.out.println("\n  Passing Plain object to JS function:");
            String result = testPassToJS(plainObj);
            System.out.println("  Result from JS: " + result);

            // Test the symbol constants
            System.out.println("\n  Symbol constants from interface:");
            System.out.println("  $firstName = " + PlainTestDTO_I.$firstName);
            System.out.println("  $age = " + PlainTestDTO_I.$age);
            System.out.println("  FIELDS = " + PlainTestDTO_I.FIELDS);

        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nDemo completed successfully!");
    }

    /**
     * Test what happens when we try to pass a Java object to JavaScript
     */
    @JSBody(params = {"obj"},
            script = "console.log('JS received:', obj); " +
                     "console.log('Type:', typeof obj); " +
                     "console.log('Has firstName?', obj.firstName); " +
                     "console.log('Constructor:', obj.constructor.name); " +
                     "return obj.firstName || 'NO PROPERTY ACCESS';")
    private static native String testPassToJS(Object obj);

    /**
     * Creates a JavaScript object that conforms to our TeaVMTestDTO_I interface.
     * This demonstrates creating JS objects from Java that can be used seamlessly.
     * Note: age parameter is Integer to support nullable values.
     */
    @JSBody(params = {"firstName", "lastName", "age", "email", "field"},
            script = "return {firstName: firstName, lastName: lastName, age: age, email: email, addedNewField: field};")
    private static native TeaVMTestDTO_I<?> createPersonFromJS(String firstName, String lastName,
                                                                Integer age, String email, String field);

    /**
     * Creates a JavaScript array of person objects.
     */
    @JSBody(script = "return ["
            + "{firstName: 'Alice', lastName: 'Johnson', age: 28, email: 'alice@example.com', addedNewField: 'Designer'},"
            + "{firstName: 'Bob', lastName: 'Williams', age: 35, email: 'bob@example.com', addedNewField: 'Manager'},"
            + "{firstName: 'Charlie', lastName: 'Brown', age: 42, email: 'charlie@example.com', addedNewField: 'Architect'}"
            + "];")
    private static native JSArray<TeaVMTestDTO_I<?>> createPeopleArray();

    /**
     * Displays person data in the HTML DOM to demonstrate browser integration.
     */
    private static void displayInDOM(TeaVMTestDTO_I<?> person) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement body = document.getBody();

        // Create a container div
        HTMLElement div = document.createElement("div");
        div.setAttribute("style", "font-family: Arial, sans-serif; padding: 20px; background-color: #f0f0f0; border-radius: 8px; margin: 20px;");

        // Add title
        HTMLElement title = document.createElement("h2");
        title.setInnerHTML("TeaVM + Generated DTO Interface Demo");
        div.appendChild(title);

        // Add person info
        String ageDisplay = integerToString(person.getAge());
        HTMLElement info = document.createElement("div");
        info.setInnerHTML(
            "<p><strong>Name:</strong> " + person.getFirstName() + " " + person.getLastName() + "</p>" +
            "<p><strong>Age:</strong> " + ageDisplay + "</p>" +
            "<p><strong>Email:</strong> " + person.getEmail() + "</p>" +
            "<p><strong>Field:</strong> " + person.getAddedNewField() + "</p>" +
            "<p style='color: green;'><strong>✓ Interface extends JSObject</strong></p>" +
            "<p style='color: green;'><strong>✓ Getters/Setters have @JSProperty</strong></p>" +
            "<p style='color: green;'><strong>✓ Seamless Java-JavaScript interop working!</strong></p>" +
            "<p style='color: blue;'><strong>✓ Using Integer for nullable age field!</strong></p>"
        );
        div.appendChild(info);

        body.appendChild(div);
    }

    /**
     * Helper method to convert Integer to String in TeaVM.
     * TeaVM doesn't support wrapper object methods like .toString() or .intValue()
     * because JavaScript numbers are primitives. This helper uses JSBody to safely
     * convert the value by directly accessing the JavaScript primitive.
     */
    private static String integerToString(Integer value) {
        if (value == null) {
            return "null";
        }
        // Use JSBody to convert the number to string in JavaScript
        return convertNumberToString(value);
    }

    /**
     * Converts a number (Integer/Double/etc.) to String using JavaScript's String() function.
     */
    @JSBody(params = {"num"}, script = "return String(num);")
    private static native String convertNumberToString(Object num);

    /**
     * Test: Try to access the $value field directly from jl_Integer wrapper.
     */
    @JSBody(params = {"num"}, script = "return num.$value;")
    private static native int getValueFromInteger(Integer num);

    /**
     * Alternative test: Direct type cast approach.
     */
    private static String integerToStringViaValue(Integer value) {
        if (value == null) {
            return "null";
        }
        // Try to get the internal value
        int intVal = getValueFromInteger(value);
        return String.valueOf(intVal);
    }
}
