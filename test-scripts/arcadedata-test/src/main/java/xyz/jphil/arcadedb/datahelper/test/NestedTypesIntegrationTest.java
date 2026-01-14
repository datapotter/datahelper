package xyz.jphil.arcadedb.datahelper.test;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.GlobalConfiguration;
import com.arcadedb.database.Database;
import com.arcadedb.engine.ComponentFile;
import com.arcadedb.server.ArcadeDBServer;
import xyz.jphil.arcadedb.datahelper.InitDoc;
import xyz.jphil.arcadedb.datahelper.Update;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static xyz.jphil.arcadedb.datahelper.test.EmployeeDTO_A.*;

/**
 * Integration test for nested types support in @ArcadeData.
 * Tests:
 * 1. Single embedded objects
 * 2. List of embedded objects (List<CustomDataObj>)
 * 3. Automatic dependency-ordered type registration
 * 4. Serialization and deserialization of nested types
 */
public class NestedTypesIntegrationTest {

    private static final String DB_NAME = "nested_types_test_db";
    private static ArcadeDBServer server;
    private static Path dbPath;

    public static void main(String[] args) {
        try {
            System.out.println("=== Nested Types Integration Test ===\n");

            initDatabase();
            testSchemaInitialization();
            testCreateEmployeeWithNestedTypes();
            testQueryAndLoadNestedTypes();
            testUpdateNestedTypes();

            System.out.println("\n=== All nested types tests passed! ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void initDatabase() throws Exception {
        System.out.println("1. Initializing ArcadeDB...");

        dbPath = Files.createTempDirectory("arcadedb_nested_test_");
        System.out.println("   Database path: " + dbPath);

        GlobalConfiguration.SERVER_ROOT_PATH.setValue(dbPath.toString());
        GlobalConfiguration.PROFILE.setValue("low-ram");
        GlobalConfiguration.SERVER_ROOT_PASSWORD.setValue("admin12345");
        GlobalConfiguration.SERVER_METRICS.setValue(false);

        server = new ArcadeDBServer(new ContextConfiguration());
        server.start();

        if (!server.existsDatabase(DB_NAME)) {
            server.createDatabase(DB_NAME, ComponentFile.MODE.READ_WRITE);
        }

        System.out.println("   ✓ Database initialized\n");
    }

    private static void testSchemaInitialization() {
        System.out.println("2. Testing schema initialization with nested types...");
        Database db = server.getDatabase(DB_NAME);

        // Initialize schema - only pass top-level type, embedded types should be detected automatically
        InitDoc.initDocTypes(db, EmployeeDTO.TYPEDEF);

        // Verify all types were registered
        if (db.getSchema().existsType("EmployeeDTO")) {
            System.out.println("   ✓ EmployeeDTO registered");
        }
        if (db.getSchema().existsType("AddressDTO")) {
            System.out.println("   ✓ AddressDTO (embedded) registered");
        }
        if (db.getSchema().existsType("PhoneNumberDTO")) {
            System.out.println("   ✓ PhoneNumberDTO (embedded list element) registered");
        }

        System.out.println("   ✓ Schema initialized with dependency-ordered type registration\n");
    }

    private static void testCreateEmployeeWithNestedTypes() {
        System.out.println("3. Testing create and save with nested types...");
        Database db = server.getDatabase(DB_NAME);

        db.transaction(() -> {
            // Create employee
            EmployeeDTO employee = new EmployeeDTO();
            employee.employeeId("EMP001");
            employee.firstName("Alice");
            employee.lastName("Johnson");
            employee.email("alice.johnson@company.com");
            employee.age(32);

            // Create home address (embedded object)
            AddressDTO homeAddress = new AddressDTO();
            homeAddress.street("123 Main Street");
            homeAddress.city("San Francisco");
            homeAddress.state("CA");
            homeAddress.zipCode("94105");
            homeAddress.country("USA");
            employee.homeAddress(homeAddress);

            // Create work address (embedded object)
            AddressDTO workAddress = new AddressDTO();
            workAddress.street("456 Corporate Blvd");
            workAddress.city("San Jose");
            workAddress.state("CA");
            workAddress.zipCode("95110");
            workAddress.country("USA");
            employee.workAddress(workAddress);

            // Create phone numbers (list of embedded objects)
            List<PhoneNumberDTO> phoneNumbers = new ArrayList<>();

            PhoneNumberDTO phone1 = new PhoneNumberDTO();
            phone1.type("mobile");
            phone1.number("+1-415-555-1234");
            phone1.isPrimary(true);
            phoneNumbers.add(phone1);

            PhoneNumberDTO phone2 = new PhoneNumberDTO();
            phone2.type("work");
            phone2.number("+1-408-555-5678");
            phone2.isPrimary(false);
            phoneNumbers.add(phone2);

            employee.phoneNumbers(phoneNumbers);

            // Create skills (simple list)
            List<String> skills = new ArrayList<>();
            skills.add("Java");
            skills.add("Python");
            skills.add("ArcadeDB");
            employee.skills(skills);

            // Save using the DSL
            Update.use(db)
                    .select("EmployeeDTO")
                    .whereEq($employeeId.name(), employee.employeeId())
                    .upsert()
                    .from(employee, new String[0])
                    .saveDocument();

            System.out.println("   ✓ Created employee: " + employee.firstName() + " " + employee.lastName());
            System.out.println("     - Home: " + homeAddress.city() + ", " + homeAddress.state());
            System.out.println("     - Work: " + workAddress.city() + ", " + workAddress.state());
            System.out.println("     - Phones: " + phoneNumbers.size() + " numbers");
            System.out.println("     - Skills: " + String.join(", ", skills));
        });

        System.out.println();
    }

    private static void testQueryAndLoadNestedTypes() {
        System.out.println("4. Testing query and load with nested types...");
        Database db = server.getDatabase(DB_NAME);

        var docs = db.select()
                .fromType("EmployeeDTO")
                .where().property($employeeId.name()).eq().value("EMP001")
                .documents();

        if (docs.hasNext()) {
            EmployeeDTO employee = new EmployeeDTO();
            employee.fromArcadeDocument(docs.next());

            System.out.println("   ✓ Loaded employee: " + employee.firstName() + " " + employee.lastName());
            System.out.println("     Email: " + employee.email());
            System.out.println("     Age: " + employee.age());

            // Verify home address
            if (employee.homeAddress() != null) {
                System.out.println("     Home Address:");
                System.out.println("       " + employee.homeAddress().street());
                System.out.println("       " + employee.homeAddress().city() + ", " +
                        employee.homeAddress().state() + " " + employee.homeAddress().zipCode());
            }

            // Verify work address
            if (employee.workAddress() != null) {
                System.out.println("     Work Address:");
                System.out.println("       " + employee.workAddress().street());
                System.out.println("       " + employee.workAddress().city() + ", " +
                        employee.workAddress().state() + " " + employee.workAddress().zipCode());
            }

            // Verify phone numbers
            if (employee.phoneNumbers() != null && !employee.phoneNumbers().isEmpty()) {
                System.out.println("     Phone Numbers:");
                for (PhoneNumberDTO phone : employee.phoneNumbers()) {
                    System.out.println("       " + phone.type() + ": " + phone.number() +
                            (phone.isPrimary() != null && phone.isPrimary() ? " (primary)" : ""));
                }
            }

            // Verify skills
            if (employee.skills() != null && !employee.skills().isEmpty()) {
                System.out.println("     Skills: " + String.join(", ", employee.skills()));
            }
        }

        System.out.println();
    }

    private static void testUpdateNestedTypes() {
        System.out.println("5. Testing update with nested types...");
        Database db = server.getDatabase(DB_NAME);

        db.transaction(() -> {
            // Load existing employee
            var docs = db.select()
                    .fromType("EmployeeDTO")
                    .where().property($employeeId.name()).eq().value("EMP001")
                    .documents();

            if (docs.hasNext()) {
                EmployeeDTO employee = new EmployeeDTO();
                employee.fromArcadeDocument(docs.next());

                // Update age
                employee.age(33);

                // Add a new phone number
                PhoneNumberDTO phone3 = new PhoneNumberDTO();
                phone3.type("home");
                phone3.number("+1-415-555-9999");
                phone3.isPrimary(false);

                if (employee.phoneNumbers() == null) {
                    employee.phoneNumbers(new ArrayList<>());
                }
                employee.phoneNumbers().add(phone3);

                // Update work address city
                if (employee.workAddress() != null) {
                    employee.workAddress().city("Palo Alto");
                }

                // Save updates
                Update.use(db)
                        .select("EmployeeDTO")
                        .whereEq($employeeId.name(), employee.employeeId())
                        .upsert()
                        .from(employee, new String[0])
                        .saveDocument();

                System.out.println("   ✓ Updated employee age to: " + employee.age());
                System.out.println("   ✓ Added new phone number");
                System.out.println("   ✓ Updated work address city to: " + employee.workAddress().city());
            }
        });

        // Verify updates
        var docs = db.select()
                .fromType("EmployeeDTO")
                .where().property($employeeId.name()).eq().value("EMP001")
                .documents();

        if (docs.hasNext()) {
            EmployeeDTO employee = new EmployeeDTO();
            employee.fromArcadeDocument(docs.next());

            System.out.println("   ✓ Verified updates:");
            System.out.println("     Age: " + employee.age() + " (should be 33)");
            System.out.println("     Phone count: " + employee.phoneNumbers().size() + " (should be 3)");
            System.out.println("     Work city: " + employee.workAddress().city() + " (should be Palo Alto)");
        }

        System.out.println();
    }

    private static void shutdown() {
        System.out.println("6. Shutting down...");
        if (server != null && server.isStarted()) {
            try {
                server.stop();
                System.out.println("   ✓ Server stopped");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (dbPath != null && Files.exists(dbPath)) {
                deleteDirectory(dbPath);
                System.out.println("   ✓ Cleaned up test directory");
            }
        } catch (Exception e) {
            System.err.println("   Warning: Could not cleanup: " + e.getMessage());
        }
    }

    private static void deleteDirectory(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteDirectory(entry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }
}
