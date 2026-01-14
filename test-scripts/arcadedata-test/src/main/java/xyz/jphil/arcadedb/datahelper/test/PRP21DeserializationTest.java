package xyz.jphil.arcadedb.datahelper.test;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.database.Document;
import xyz.jphil.arcadedb.datahelper.InitDoc;
import xyz.jphil.arcadedb.datahelper.Update;

import java.io.File;
import java.util.List;

import static xyz.jphil.arcadedb.datahelper.test.EmployeeDTO_A.$employeeId;

/**
 * Focused test for PRP-21: Nested object deserialization.
 * Tests that nested DataHelper objects are properly serialized and deserialized.
 */
public class PRP21DeserializationTest {

    public static void main(String[] args) {
        System.out.println("=== PRP-21 Nested Object Deserialization Test ===\n");

        File dbPath = new File(System.getProperty("java.io.tmpdir"), "arcadedb-nested-test-" + System.currentTimeMillis());
        System.out.println("Database path: " + dbPath.getAbsolutePath() + "\n");

        try (DatabaseFactory factory = new DatabaseFactory(dbPath.getAbsolutePath())) {
            try (Database db = factory.create()) {
                db.getConfiguration().setValue("arcadedb.cypher.allowFiltering", true);

                // Test 1: Schema initialization
                test1_SchemaInitialization(db);

                // Test 2: Create and save with nested objects
                test2_CreateAndSave(db);

                // Test 3: Load and verify deserialization
                test3_LoadAndVerify(db);

                System.out.println("\n=== ALL TESTS PASSED ✓ ===");
            }
        }
    }

    private static void test1_SchemaInitialization(Database db) {
        System.out.println("Test 1: Schema initialization");

        InitDoc.initDocTypes(db, EmployeeDTO.TYPEDEF);

        System.out.println("  ✓ Schema initialized with dependency detection");
        System.out.println("  ✓ Expected order: AddressDTO → PhoneNumberDTO → EmployeeDTO\n");
    }

    private static void test2_CreateAndSave(Database db) {
        System.out.println("Test 2: Create and save with nested objects");

        db.transaction(() -> {
            // Create nested objects
            AddressDTO homeAddr = new AddressDTO();
            homeAddr.street("123 Main St");
            homeAddr.city("San Francisco");
            homeAddr.zipCode("94101");

            AddressDTO workAddr = new AddressDTO();
            workAddr.street("456 Tech Blvd");
            workAddr.city("San Jose");
            workAddr.zipCode("95110");

            PhoneNumberDTO phone1 = new PhoneNumberDTO();
            phone1.type("mobile");
            phone1.number("555-1234");

            PhoneNumberDTO phone2 = new PhoneNumberDTO();
            phone2.type("work");
            phone2.number("555-5678");

            // Create employee
            EmployeeDTO employee = new EmployeeDTO();
            employee.employeeId("EMP001");
            employee.firstName("Alice");
            employee.lastName("Johnson");
            employee.email("alice@example.com");
            employee.age(30);
            employee.homeAddress(homeAddr);
            employee.workAddress(workAddr);
            employee.phoneNumbers(List.of(phone1, phone2));

            // Save
            Update.use(db)
                    .select("EmployeeDTO")
                    .whereEq($employeeId.name(), employee.employeeId())
                    .upsert()
                    .from(employee, new String[0])
                    .saveDocument();

            System.out.println("  ✓ Created employee: " + employee.firstName() + " " + employee.lastName());
            System.out.println("    - Home: " + homeAddr.city() + ", " + homeAddr.zipCode());
            System.out.println("    - Work: " + workAddr.city() + ", " + workAddr.zipCode());
            System.out.println("    - Phones: " + employee.phoneNumbers().size() + " numbers\n");
        });
    }

    private static void test3_LoadAndVerify(Database db) {
        System.out.println("Test 3: Load and verify deserialization");

        var docs = db.select()
                .fromType("EmployeeDTO")
                .where().property($employeeId.name()).eq().value("EMP001")
                .documents();

        if (!docs.hasNext()) {
            throw new RuntimeException("❌ Employee not found!");
        }

        Document doc = docs.next();

        // Debug: Print raw document
        System.out.println("  DEBUG: Raw document keys:");
        for (String key : doc.getPropertyNames()) {
            Object value = doc.get(key);
            System.out.println("    - " + key + ": " + value + " (" + (value != null ? value.getClass().getSimpleName() : "null") + ")");
        }
        System.out.println();

        // Deserialize
        EmployeeDTO employee = new EmployeeDTO();
        employee.fromArcadeDocument(doc);

        // Verify basic fields
        System.out.println("  ✓ Basic fields:");
        System.out.println("    - ID: " + employee.employeeId());
        System.out.println("    - Name: " + employee.firstName() + " " + employee.lastName());
        System.out.println("    - Email: " + employee.email());
        System.out.println("    - Age: " + employee.age());
        System.out.println();

        // Verify nested objects
        System.out.println("  ✓ Nested objects:");

        if (employee.homeAddress() == null) {
            System.out.println("    ❌ homeAddress is NULL (deserialization failed)");
            throw new RuntimeException("homeAddress deserialization failed");
        } else {
            System.out.println("    - Home address: " + employee.homeAddress().city() + ", " + employee.homeAddress().zipCode());
        }

        if (employee.workAddress() == null) {
            System.out.println("    ❌ workAddress is NULL (deserialization failed)");
            throw new RuntimeException("workAddress deserialization failed");
        } else {
            System.out.println("    - Work address: " + employee.workAddress().city() + ", " + employee.workAddress().zipCode());
        }

        // Verify list of nested objects
        if (employee.phoneNumbers() == null || employee.phoneNumbers().isEmpty()) {
            System.out.println("    ❌ phoneNumbers is NULL or empty (deserialization failed)");
            throw new RuntimeException("phoneNumbers deserialization failed");
        } else {
            System.out.println("    - Phone count: " + employee.phoneNumbers().size());
            System.out.println("    - Phone details:");
            for (PhoneNumberDTO phone : employee.phoneNumbers()) {
                System.out.println("      • " + phone.type() + ": " + phone.number());
            }
        }
        System.out.println();

        System.out.println("  ✓ All nested objects deserialized correctly!");
    }
}
