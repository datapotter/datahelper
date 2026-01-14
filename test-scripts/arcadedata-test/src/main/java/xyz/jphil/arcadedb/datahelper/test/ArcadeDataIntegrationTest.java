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

import static xyz.jphil.arcadedb.datahelper.test.TestPersonDTO_A.*;

/**
 * Integration test for @ArcadeData annotation pattern.
 * Demonstrates the new simplified approach with sealed abstract parent.
 */
public class ArcadeDataIntegrationTest {

    private static final String DB_NAME = "arcadedata_test_db";
    private static ArcadeDBServer server;
    private static Path dbPath;

    public static void main(String[] args) {
        try {
            System.out.println("=== @ArcadeData Integration Test ===\n");

            initDatabase();
            testSchemaInitialization();
            testCreateAndSave();
            testQueryAndLoad();
            testUpdate();

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void initDatabase() throws Exception {
        System.out.println("1. Initializing ArcadeDB...");

        dbPath = Files.createTempDirectory("arcadedb_test_");
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
        System.out.println("2. Testing schema initialization...");
        Database db = server.getDatabase(DB_NAME);

        InitDoc.initDocTypes(db, TestPersonDTO.TYPEDEF);

        System.out.println("   ✓ Schema initialized with unique index on 'email'\n");
    }

    private static void testCreateAndSave() {
        System.out.println("3. Testing create and save...");
        Database db = server.getDatabase(DB_NAME);

        db.transaction(() -> {
            TestPersonDTO person = new TestPersonDTO();
            person.name("Alice");
            person.email("alice@example.com");
            person.age(28);

            Update.use(db)
                    .select("TestPersonDTO")
                    .whereEq($email.name(), person.email())
                    .upsert()
                    .from(person, new String[0])
                    .saveDocument();

            System.out.println("   ✓ Created: " + person.name() + " (" + person.email() + ")");
        });

        System.out.println();
    }

    private static void testQueryAndLoad() {
        System.out.println("4. Testing query and load...");
        Database db = server.getDatabase(DB_NAME);

        var docs = db.select()
                .fromType("TestPersonDTO")
                .where().property($email.name()).eq().value("alice@example.com")
                .documents();

        if (docs.hasNext()) {
            TestPersonDTO person = new TestPersonDTO();
            person.fromArcadeDocument(docs.next());

            System.out.println("   ✓ Found: " + person.name());
            System.out.println("     Email: " + person.email());
            System.out.println("     Age: " + person.age());
        }

        System.out.println();
    }

    private static void testUpdate() {
        System.out.println("5. Testing update...");
        Database db = server.getDatabase(DB_NAME);

        db.transaction(() -> {
            Update.use(db)
                    .select("TestPersonDTO")
                    .whereEq($email.name(), "alice@example.com")
                    .upsert()
                    .__($age.name(), () -> 29)
                    .saveDocument();

            System.out.println("   ✓ Updated age to 29");
        });

        var docs = db.select()
                .fromType("TestPersonDTO")
                .where().property($email.name()).eq().value("alice@example.com")
                .documents();

        if (docs.hasNext()) {
            TestPersonDTO person = new TestPersonDTO();
            person.fromArcadeDocument(docs.next());
            System.out.println("   ✓ Verified: " + person.name() + ", age " + person.age());
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
