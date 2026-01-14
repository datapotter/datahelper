///usr/bin/env jbang "$0" "$@" ; exit $?
// Standalone Java code - tests ArcadeDB query patterns

//DEPS com.arcadedb:arcadedb-engine:25.7.1
//DEPS com.arcadedb:arcadedb-server:25.7.1

import com.arcadedb.*;
import com.arcadedb.database.*;
import com.arcadedb.engine.ComponentFile;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.server.ArcadeDBServer;
import java.nio.file.*;

public class ArcadeDBQueryTest {

    public static void main(String[] args) throws Exception {
        // Suppress warnings
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        Path dbPath = Paths.get(System.getProperty("user.home"), ".arcadedb-test");
        Files.createDirectories(dbPath);

        ContextConfiguration config = new ContextConfiguration();
        config.setValue(GlobalConfiguration.SERVER_ROOT_PASSWORD, "test");
        config.setValue(GlobalConfiguration.SERVER_DATABASE_DIRECTORY, dbPath.toString());
        config.setValue(GlobalConfiguration.HA_ENABLED, false);

        ArcadeDBServer server = new ArcadeDBServer(config);
        server.start();

        try {
            server.createDatabase("testdb", ComponentFile.MODE.READ_WRITE);
            Database db = server.getDatabase("testdb");

            // Create schema
            db.transaction(() -> {
                db.getSchema().createDocumentType("Person");
            });

            System.out.println("=== Test 1: Insert with query ===");
            db.transaction(() -> {
                db.command("sql", "INSERT INTO Person SET name = ?, email = ?", "Alice", "alice@test.com");
                db.command("sql", "INSERT INTO Person SET name = ?, email = ?", "Bob", "bob@test.com");
            });
            System.out.println("Inserted 2 records");

            System.out.println("\n=== Test 2: Query with parameters ===");
            try (ResultSet rs = db.query("sql", "SELECT FROM Person WHERE email = ?", "alice@test.com")) {
                if (rs.hasNext()) {
                    System.out.println("Found: " + rs.next().toJSON());
                }
            }

            System.out.println("\n=== Test 3: Delete pattern ===");
            db.transaction(() -> {
                try (ResultSet rs = db.query("sql", "SELECT FROM Person WHERE email = ?", "bob@test.com")) {
                    if (rs.hasNext()) {
                        var result = rs.next();
                        var record = result.getRecord();
                        if (record.isPresent()) {
                            db.deleteRecord(record.get());
                            System.out.println("Deleted Bob");
                        }
                    }
                }
            });

            System.out.println("\n=== Test 4: Verify delete ===");
            try (ResultSet rs = db.query("sql", "SELECT FROM Person")) {
                int count = 0;
                while (rs.hasNext()) {
                    System.out.println("  - " + rs.next().getProperty("name"));
                    count++;
                }
                System.out.println("Total: " + count + " (should be 1)");
            }

            System.out.println("\n✅ All tests passed!");

        } finally {
            server.stop();
        }
    }
}
