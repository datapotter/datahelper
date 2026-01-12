package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSMapLike;
import xyz.jphil.datahelper.teavm.TeaVMMapLike;

/**
 * Comprehensive test for nested objects in TeaVM.
 * Tests the MapLike abstraction with complex nested structures.
 *
 * This demonstrates:
 * 1. Simple nested objects (Person -> Address)
 * 2. Multiple nested objects (Person -> homeAddress, workAddress)
 * 3. Deep nesting (Person -> Company -> Address)
 * 4. Null handling at various nesting levels
 * 5. Serialization back to JSON (toMapLike)
 */
public class NestedObjectTest {

    public static void main(String[] args) {
        log("=== Nested Object Tests ===");
        testSimpleNestedObject();
        testMultipleNestedObjects();
        testDeepNesting();
        testNullNestedObjects();
        testSerializationWithNesting();
        log("=== All tests completed! ===");
    }

    /**
     * Test 1: Simple nested object (Person with homeAddress only)
     */
    private static void testSimpleNestedObject() {
        log("\n--- Test 1: Simple Nested Object ---");

        String json = "{"
            + "\"firstName\":\"Alice\","
            + "\"lastName\":\"Smith\","
            + "\"age\":30,"
            + "\"email\":\"alice@example.com\","
            + "\"homeAddress\":{"
            + "  \"street\":\"123 Main St\","
            + "  \"city\":\"Springfield\","
            + "  \"state\":\"IL\","
            + "  \"zipCode\":\"62701\","
            + "  \"country\":\"USA\""
            + "}"
            + "}";

        JSMapLike<JSObject> personJson = (JSMapLike<JSObject>) parseJSON(json);
        PersonDTO person = new PersonDTO();
        person.fromMapLike(TeaVMMapLike.wrap(personJson));

        log("Person: " + person.getFirstName() + " " + person.getLastName());
        log("Age: " + person.getAge());
        log("Email: " + person.getEmail());

        AddressDTO addr = person.getHomeAddress();
        if (addr != null) {
            log("Home Address:");
            log("  Street: " + addr.getStreet());
            log("  City: " + addr.getCity());
            log("  State: " + addr.getState());
            log("  Zip: " + addr.getZipCode());
            log("  Country: " + addr.getCountry());
        } else {
            log("ERROR: homeAddress should not be null!");
        }
    }

    /**
     * Test 2: Multiple nested objects (Person with both homeAddress and workAddress)
     */
    private static void testMultipleNestedObjects() {
        log("\n--- Test 2: Multiple Nested Objects ---");

        String json = "{"
            + "\"firstName\":\"Bob\","
            + "\"lastName\":\"Johnson\","
            + "\"age\":35,"
            + "\"homeAddress\":{"
            + "  \"street\":\"456 Oak Ave\","
            + "  \"city\":\"Chicago\","
            + "  \"state\":\"IL\","
            + "  \"zipCode\":\"60601\","
            + "  \"country\":\"USA\""
            + "},"
            + "\"workAddress\":{"
            + "  \"street\":\"789 Corporate Blvd\","
            + "  \"city\":\"Chicago\","
            + "  \"state\":\"IL\","
            + "  \"zipCode\":\"60602\","
            + "  \"country\":\"USA\""
            + "}"
            + "}";

        JSMapLike<JSObject> personJson = (JSMapLike<JSObject>) parseJSON(json);
        PersonDTO person = new PersonDTO();
        person.fromMapLike(TeaVMMapLike.wrap(personJson));

        log("Person: " + person.getFirstName() + " " + person.getLastName());

        AddressDTO home = person.getHomeAddress();
        if (home != null) {
            log("Home: " + home.getStreet() + ", " + home.getCity());
        }

        AddressDTO work = person.getWorkAddress();
        if (work != null) {
            log("Work: " + work.getStreet() + ", " + work.getCity());
        }
    }

    /**
     * Test 3: Deep nesting (Person -> Company -> Address)
     */
    private static void testDeepNesting() {
        log("\n--- Test 3: Deep Nesting (Person -> Company -> Address) ---");

        String json = "{"
            + "\"firstName\":\"Charlie\","
            + "\"lastName\":\"Brown\","
            + "\"age\":28,"
            + "\"email\":\"charlie@techcorp.com\","
            + "\"company\":{"
            + "  \"name\":\"TechCorp\","
            + "  \"department\":\"Engineering\","
            + "  \"employeeCount\":500,"
            + "  \"address\":{"
            + "    \"street\":\"1000 Tech Drive\","
            + "    \"city\":\"San Francisco\","
            + "    \"state\":\"CA\","
            + "    \"zipCode\":\"94105\","
            + "    \"country\":\"USA\""
            + "  }"
            + "}"
            + "}";

        JSMapLike<JSObject> personJson = (JSMapLike<JSObject>) parseJSON(json);
        PersonDTO person = new PersonDTO();
        person.fromMapLike(TeaVMMapLike.wrap(personJson));

        log("Person: " + person.getFirstName() + " " + person.getLastName());
        log("Email: " + person.getEmail());

        CompanyDTO company = person.getCompany();
        if (company != null) {
            log("Company: " + company.getName());
            log("Department: " + company.getDepartment());
            log("Employees: " + company.getEmployeeCount());

            AddressDTO companyAddr = company.getAddress();
            if (companyAddr != null) {
                log("Company Address:");
                log("  " + companyAddr.getStreet());
                log("  " + companyAddr.getCity() + ", " + companyAddr.getState() + " " + companyAddr.getZipCode());
            } else {
                log("ERROR: company.address should not be null!");
            }
        } else {
            log("ERROR: company should not be null!");
        }
    }

    /**
     * Test 4: Null/missing nested objects at various levels
     */
    private static void testNullNestedObjects() {
        log("\n--- Test 4: Null/Missing Nested Objects ---");

        // Test 4a: Missing optional nested object (workAddress)
        String json1 = "{"
            + "\"firstName\":\"Dave\","
            + "\"lastName\":\"Wilson\","
            + "\"homeAddress\":{"
            + "  \"street\":\"111 Home St\","
            + "  \"city\":\"Boston\","
            + "  \"state\":\"MA\","
            + "  \"zipCode\":\"02101\","
            + "  \"country\":\"USA\""
            + "}"
            + "}";

        JSMapLike<JSObject> person1Json = (JSMapLike<JSObject>) parseJSON(json1);
        PersonDTO person1 = new PersonDTO();
        person1.fromMapLike(TeaVMMapLike.wrap(person1Json));

        log("Test 4a - Missing workAddress:");
        log("  Name: " + person1.getFirstName() + " " + person1.getLastName());
        log("  homeAddress: " + (person1.getHomeAddress() != null ? "present" : "null"));
        log("  workAddress: " + (person1.getWorkAddress() != null ? "present" : "null"));

        // Test 4b: Explicit null nested object
        String json2 = "{"
            + "\"firstName\":\"Eve\","
            + "\"lastName\":\"Taylor\","
            + "\"age\":null,"
            + "\"company\":null"
            + "}";

        JSMapLike<JSObject> person2Json = (JSMapLike<JSObject>) parseJSON(json2);
        PersonDTO person2 = new PersonDTO();
        person2.fromMapLike(TeaVMMapLike.wrap(person2Json));

        log("Test 4b - Explicit nulls:");
        log("  Name: " + person2.getFirstName() + " " + person2.getLastName());
        log("  age: " + person2.getAge());
        log("  company: " + (person2.getCompany() != null ? "present" : "null"));

        // Test 4c: Company present but nested address missing
        String json3 = "{"
            + "\"firstName\":\"Frank\","
            + "\"lastName\":\"Miller\","
            + "\"company\":{"
            + "  \"name\":\"StartupCo\","
            + "  \"department\":\"Sales\","
            + "  \"employeeCount\":10"
            + "}"
            + "}";

        JSMapLike<JSObject> person3Json = (JSMapLike<JSObject>) parseJSON(json3);
        PersonDTO person3 = new PersonDTO();
        person3.fromMapLike(TeaVMMapLike.wrap(person3Json));

        log("Test 4c - Company without address:");
        log("  Name: " + person3.getFirstName() + " " + person3.getLastName());
        CompanyDTO company = person3.getCompany();
        if (company != null) {
            log("  Company: " + company.getName());
            log("  Company.address: " + (company.getAddress() != null ? "present" : "null"));
        }
    }

    /**
     * Test 5: Serialization with nested objects (round-trip via MapLike)
     */
    private static void testSerializationWithNesting() {
        log("\n--- Test 5: Serialization with Nested Objects ---");

        // Build objects programmatically
        AddressDTO homeAddr = new AddressDTO()
            .street("999 Test Lane")
            .city("Austin")
            .state("TX")
            .zipCode("78701")
            .country("USA");

        AddressDTO companyAddr = new AddressDTO()
            .street("2000 Business Park")
            .city("Austin")
            .state("TX")
            .zipCode("78702")
            .country("USA");

        CompanyDTO company = new CompanyDTO()
            .name("DataCorp")
            .department("Research")
            .employeeCount(250)
            .address(companyAddr);

        PersonDTO original = new PersonDTO()
            .firstName("Grace")
            .lastName("Hopper")
            .age(85)
            .email("grace@datacorp.com")
            .homeAddress(homeAddr)
            .company(company);

        log("Original object created programmatically:");
        log("  Name: " + original.getFirstName() + " " + original.getLastName());
        log("  Age: " + original.getAge());
        log("  Email: " + original.getEmail());

        // Test round-trip: toMapLike() -> fromMapLike()
        // This tests that nested objects serialize and deserialize correctly
        PersonDTO roundTrip = new PersonDTO();
        roundTrip.fromMapLike(original.toMapLike(false));

        log("\nAfter round-trip (toMapLike -> fromMapLike):");
        log("  Name: " + roundTrip.getFirstName() + " " + roundTrip.getLastName());
        log("  Age: " + roundTrip.getAge());
        log("  Email: " + roundTrip.getEmail());

        // Verify nested objects
        AddressDTO rtHomeAddr = roundTrip.getHomeAddress();
        if (rtHomeAddr != null) {
            log("  Home Address: " + rtHomeAddr.getStreet() + ", " + rtHomeAddr.getCity());
        } else {
            log("  ERROR: homeAddress is null after round-trip!");
        }

        CompanyDTO rtCompany = roundTrip.getCompany();
        if (rtCompany != null) {
            log("  Company: " + rtCompany.getName() + ", " + rtCompany.getDepartment());
            log("  Employees: " + rtCompany.getEmployeeCount());

            AddressDTO rtCompanyAddr = rtCompany.getAddress();
            if (rtCompanyAddr != null) {
                log("  Company Address: " + rtCompanyAddr.getStreet() + ", " + rtCompanyAddr.getCity());
            } else {
                log("  ERROR: company.address is null after round-trip!");
            }
        } else {
            log("  ERROR: company is null after round-trip!");
        }
    }

    @JSBody(params = {"jsonString"}, script = "return JSON.parse(jsonString);")
    private static native JSObject parseJSON(String jsonString);

    @JSBody(params = {"message"}, script = "console.log(message);")
    private static native void log(String message);
}
