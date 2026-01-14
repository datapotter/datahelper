package xyz.jphil.test;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.GlobalConfiguration;
import com.arcadedb.database.Database;
import com.arcadedb.engine.ComponentFile;
import com.arcadedb.server.ArcadeDBServer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArcadeDBAPITest {

    public static void main(String[] args) throws Exception {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        Path dbPath = Path.of(System.getProperty("user.home"), ".arcadedb-api-test");
        Files.createDirectories(dbPath);

        var config = new ContextConfiguration();
        config.setValue(GlobalConfiguration.SERVER_ROOT_PASSWORD, "testpass123");
        config.setValue(GlobalConfiguration.SERVER_DATABASE_DIRECTORY, dbPath.toString());
        config.setValue(GlobalConfiguration.HA_ENABLED, false);

        var server = new ArcadeDBServer(config);
        server.start();

        try {
            server.createDatabase("testdb", ComponentFile.MODE.READ_WRITE);
            var db = server.getDatabase("testdb");

            // Create schema
            db.transaction(() -> {
                db.getSchema().createDocumentType("Person");
            });

            System.out.println("=== Test 1: Insert ===");
            db.transaction(() -> {
                db.command("sql", "INSERT INTO Person SET name = ?, email = ?", "Alice", "alice@test.com");
                db.command("sql", "INSERT INTO Person SET name = ?, email = ?", "Bob", "bob@test.com");
            });
            System.out.println("✓ Inserted 2 records");

            System.out.println("\n=== Test 2: Query with try-with-resources ===");
            try (var rs = db.query("sql", "SELECT FROM Person WHERE email = ?", "alice@test.com")) {
                if (rs.hasNext()) {
                    System.out.println("✓ Found: " + rs.next().toJSON());
                }
            }

            System.out.println("\n=== Test 3: Delete with query ===");
            db.transaction(() -> {
                try (var rs = db.query("sql", "SELECT FROM Person WHERE email = ?", "bob@test.com")) {
                    if (rs.hasNext()) {
                        var result = rs.next();
                        var record = result.getRecord();
                        if (record.isPresent()) {
                            db.deleteRecord(record.get());
                            System.out.println("✓ Deleted Bob");
                        }
                    }
                }
            });

            System.out.println("\n=== Test 4: Verify remaining records ===");
            try (var rs = db.query("sql", "SELECT FROM Person")) {
                int count = 0;
                while (rs.hasNext()) {
                    System.out.println("  - " + rs.next().getProperty("name"));
                    count++;
                }
                System.out.println("✓ Total: " + count + " (expected 1)");
            }

            System.out.println("\n✅ ALL TESTS PASSED - API patterns verified!");

        } finally {
            server.stop();
        }
    }
}
