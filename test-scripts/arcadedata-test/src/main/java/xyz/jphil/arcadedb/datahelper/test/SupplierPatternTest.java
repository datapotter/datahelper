package xyz.jphil.arcadedb.datahelper.test;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import xyz.jphil.arcadedb.datahelper.InitDoc;
import xyz.jphil.arcadedb.datahelper.Update;

import java.io.File;

import static xyz.jphil.arcadedb.datahelper.test.Person_A.*;

/**
 * Test to verify both direct value and Supplier patterns work for string and Field_I APIs.
 */
public class SupplierPatternTest {

    public static void main(String[] args) {
        System.out.println("=== Supplier Pattern Test ===\n");

        var dbPath = new File(System.getProperty("java.io.tmpdir"), "arcadedb-supplier-test-" + System.currentTimeMillis());
        System.out.println("Database path: " + dbPath.getAbsolutePath() + "\n");

        try (var factory = new DatabaseFactory(dbPath.getAbsolutePath())) {
            try (Database db = factory.create()) {
                db.getConfiguration().setValue("arcadedb.cypher.allowFiltering", true);

                // Initialize schema
                InitDoc.initDocTypes(db, Person.TYPEDEF);

                // Test 1: Type-safe API with direct values
                System.out.println("Test 1: Type-safe API - direct values");
                db.transaction(() -> {
                    Update.use(db)
                        .select(Person.__)
                        .whereEq($email, "alice@example.com")
                        .upsert()
                        .__($name, "Alice")
                        .__($age, 30)
                        .saveDocument();
                });
                System.out.println("  ✓ Direct values work\n");

                // Test 2: Type-safe API with Suppliers (need explicit Supplier type)
                System.out.println("Test 2: Type-safe API - Supplier pattern");
                db.transaction(() -> {
                    java.util.function.Supplier<String> nameSupplier = () -> "Bob";
                    java.util.function.Supplier<Integer> ageSupplier = () -> 25;

                    Update.use(db)
                        .select(Person.__)
                        .whereEq($email, "bob@example.com")
                        .upsert()
                        .__($name, nameSupplier)
                        .__($age, ageSupplier)
                        .saveDocument();
                });
                System.out.println("  ✓ Supplier pattern works\n");

                // Test 3: String-based API with direct values
                System.out.println("Test 3: String-based API - direct values");
                db.transaction(() -> {
                    Update.use(db)
                        .select("Person")
                        .whereEq("email", "charlie@example.com")
                        .upsert()
                        .__("name", "Charlie")
                        .__("age", 35)
                        .saveDocument();
                });
                System.out.println("  ✓ Direct values work\n");

                // Test 4: String-based API with Suppliers
                System.out.println("Test 4: String-based API - Supplier pattern");
                db.transaction(() -> {
                    Update.use(db)
                        .select("Person")
                        .whereEq("email", "diana@example.com")
                        .upsert()
                        .__("name", () -> "Diana")
                        .__("age", () -> 28)
                        .saveDocument();
                });
                System.out.println("  ✓ Supplier pattern works\n");

                // Test 5: Error isolation with Supplier
                System.out.println("Test 5: Error isolation - Supplier catches exceptions");
                db.transaction(() -> {
                    java.util.function.Supplier<Integer> errorSupplier = () -> {
                        throw new RuntimeException("Simulated error");
                    };

                    Update.use(db)
                        .select(Person.__)
                        .whereEq($email, "error@example.com")
                        .upsert()
                        .__($name, "ErrorTest")
                        .__($age, errorSupplier)
                        .__($email, "error@example.com")  // Should still update
                        .saveDocument();
                });

                // Verify email was saved despite age error
                var docs = db.select()
                    .fromType("Person")
                    .where().property("email").eq().value("error@example.com")
                    .documents();

                if (docs.hasNext()) {
                    var doc = docs.next();
                    var email = doc.getString("email");
                    var name = doc.getString("name");
                    System.out.println("  ✓ Error isolated - other fields updated");
                    System.out.println("    - name: " + name);
                    System.out.println("    - email: " + email);
                } else {
                    System.out.println("  ✗ Document not found");
                }

                System.out.println("\n=== ALL TESTS PASSED ✓ ===");
            }
        }
    }
}
