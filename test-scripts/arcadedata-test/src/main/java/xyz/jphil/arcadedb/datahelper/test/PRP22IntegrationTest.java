package xyz.jphil.arcadedb.datahelper.test;

import com.arcadedb.database.Database;
import com.arcadedb.database.DatabaseFactory;
import com.arcadedb.database.Document;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.MutableEdge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.index.Index;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.EdgeType;
import com.arcadedb.schema.Property;
import com.arcadedb.schema.Type;
import com.arcadedb.schema.VertexType;

import xyz.jphil.arcadedb.datahelper.InitDoc;

import java.io.File;
import java.util.Arrays;

/**
 * Integration test for PRP-22: Reference Types in ArcadeDB.
 * Tests all three phases:
 * - Phase 2: ArcadeType enum (DOCUMENT, VERTEX, EDGE)
 * - Phase 3: LINK support
 */
public class PRP22IntegrationTest {

    private static final String DB_PATH = "test-scripts/arcadedata-test/backups/prp22-test-db";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("PRP-22 Integration Test");
        System.out.println("========================================\n");

        // Clean up old database
        deleteDirectory(new File(DB_PATH));

        // Create new database
        DatabaseFactory factory = new DatabaseFactory(DB_PATH);
        try (Database db = factory.create()) {
            runAllTests(db);
        }

        System.out.println("\n========================================");
        System.out.println("All PRP-22 tests PASSED! ✅");
        System.out.println("========================================");
    }

    private static void runAllTests(Database db) {
        // Phase 2 Tests
        test1_DocumentTypeSchema(db);
        test2_VertexTypeSchema(db);
        test3_EdgeTypeSchema(db);

        // Phase 3 Tests
        test4_LinkFieldRegistration(db);
        test5_MultipleLinkFields(db);

        // Runtime Tests
        test6_VertexCreationAndRetrieval(db);
        test7_EdgeCreationAndTraversal(db);
        test8_LinkReferenceOperations(db);
        test9_CombinedVertexAndLink(db);
    }

    private static void test1_DocumentTypeSchema(Database db) {
        System.out.println("Test 1: DOCUMENT type schema creation");

        // Initialize Order schema (DOCUMENT type)
        InitDoc.initDocTypes(db, Order.TYPEDEF);

        // Verify schema
        DocumentType orderType = db.getSchema().getType("Order");
        assertNotNull(orderType, "Order type should exist");
        assertFalse(orderType instanceof VertexType, "Order should be DocumentType, not VertexType");
        assertFalse(orderType instanceof EdgeType, "Order should be DocumentType, not EdgeType");

        // Verify fields
        Property orderId = orderType.getProperty("orderId");
        assertNotNull(orderId, "orderId property should exist");
        assertEquals(Type.STRING, orderId.getType(), "orderId should be STRING");

        // Verify unique index on orderId
        Index[] indexes = db.getSchema().getIndexes();
        boolean hasIndex = Arrays.stream(indexes)
                .anyMatch(idx -> idx.getTypeName().equals("Order") &&
                        idx.getPropertyNames().size() == 1 &&
                        idx.getPropertyNames().get(0).equals("orderId"));
        assertTrue(hasIndex, "Unique index on orderId should exist");

        System.out.println("  ✅ DOCUMENT type schema creation works correctly\n");
    }

    private static void test2_VertexTypeSchema(Database db) {
        System.out.println("Test 2: VERTEX type schema creation");

        // Initialize Person schema (VERTEX type)
        InitDoc.initDocTypes(db, Person.TYPEDEF);

        // Verify schema
        DocumentType personType = db.getSchema().getType("Person");
        assertNotNull(personType, "Person type should exist");
        assertTrue(personType instanceof VertexType, "Person should be VertexType");

        // Verify fields
        Property name = personType.getProperty("name");
        assertNotNull(name, "name property should exist");

        Property email = personType.getProperty("email");
        assertNotNull(email, "email property should exist");

        // Verify unique index on email
        Index[] indexes = db.getSchema().getIndexes();
        boolean hasIndex = Arrays.stream(indexes)
                .anyMatch(idx -> idx.getTypeName().equals("Person") &&
                        idx.getPropertyNames().size() == 1 &&
                        idx.getPropertyNames().get(0).equals("email"));
        assertTrue(hasIndex, "Unique index on email should exist");

        System.out.println("  ✅ VERTEX type schema creation works correctly\n");
    }

    private static void test3_EdgeTypeSchema(Database db) {
        System.out.println("Test 3: EDGE type schema creation");

        // Initialize Knows schema (EDGE type)
        InitDoc.initDocTypes(db, Knows.TYPEDEF);

        // Verify schema
        DocumentType knowsType = db.getSchema().getType("Knows");
        assertNotNull(knowsType, "Knows type should exist");
        assertTrue(knowsType instanceof EdgeType, "Knows should be EdgeType");

        // Verify fields
        Property since = knowsType.getProperty("since");
        assertNotNull(since, "since property should exist");

        Property strength = knowsType.getProperty("strength");
        assertNotNull(strength, "strength property should exist");

        // Note: EdgeType has @in and @out as internal properties managed by ArcadeDB
        // We don't need to verify them explicitly

        System.out.println("  ✅ EDGE type schema creation works correctly\n");
    }

    private static void test4_LinkFieldRegistration(Database db) {
        System.out.println("Test 4: LINK field registration");

        // Debug: Check linkFields
        System.out.println("  DEBUG: linkFields count = " + Order.TYPEDEF.linkFields().size());
        if (!Order.TYPEDEF.linkFields().isEmpty()) {
            System.out.println("  DEBUG: first link field = " + Order.TYPEDEF.linkFields().get(0).name());
        }

        // Order.TYPEDEF should have registered $customerRef as LINK
        DocumentType orderType = db.getSchema().getType("Order");

        Property customerRef = orderType.getProperty("customerRef");
        assertNotNull(customerRef, "customerRef property should exist");
        assertEquals(Type.LINK, customerRef.getType(), "customerRef should be Type.LINK");

        System.out.println("  ✅ LINK field registration works correctly\n");
    }

    private static void test5_MultipleLinkFields(Database db) {
        System.out.println("Test 5: Multiple LINK fields");

        // Initialize Company schema (VERTEX with multiple LINKs)
        InitDoc.initDocTypes(db, Company.TYPEDEF);

        DocumentType companyType = db.getSchema().getType("Company");
        assertTrue(companyType instanceof VertexType, "Company should be VertexType");

        // Verify first LINK field
        Property ceoRef = companyType.getProperty("ceoRef");
        assertNotNull(ceoRef, "ceoRef property should exist");
        assertEquals(Type.LINK, ceoRef.getType(), "ceoRef should be Type.LINK");

        // Verify second LINK field
        Property founderRef = companyType.getProperty("founderRef");
        assertNotNull(founderRef, "founderRef property should exist");
        assertEquals(Type.LINK, founderRef.getType(), "founderRef should be Type.LINK");

        System.out.println("  ✅ Multiple LINK fields work correctly\n");
    }

    private static void test6_VertexCreationAndRetrieval(Database db) {
        System.out.println("Test 6: VERTEX creation and retrieval");

        db.transaction(() -> {
            // Create Person vertex
            MutableVertex alice = db.newVertex("Person");
            alice.set("name", "Alice");
            alice.set("email", "alice@example.com");
            alice.set("age", 30);
            alice.save();

            // Retrieve and verify
            Vertex retrieved = db.select().fromType("Person")
                    .where().property("email").eq().value("alice@example.com")
                    .vertices().next();

            assertNotNull(retrieved, "Should retrieve Alice");
            assertEquals("Alice", retrieved.get("name"), "Name should match");
            assertEquals(30, retrieved.get("age"), "Age should match");

            System.out.println("  ✅ VERTEX creation and retrieval works correctly\n");
        });
    }

    private static void test7_EdgeCreationAndTraversal(Database db) {
        System.out.println("Test 7: EDGE creation and traversal");

        db.transaction(() -> {
            // Create two Person vertices
            MutableVertex bob = db.newVertex("Person");
            bob.set("name", "Bob");
            bob.set("email", "bob@example.com");
            bob.set("age", 25);
            bob.save();

            MutableVertex charlie = db.newVertex("Person");
            charlie.set("name", "Charlie");
            charlie.set("email", "charlie@example.com");
            charlie.set("age", 28);
            charlie.save();

            // Create Knows edge
            MutableEdge knowsEdge = bob.newEdge("Knows", charlie,
                    new Object[]{"since", "2020", "strength", "strong"});
            knowsEdge.save();

            // Traverse and verify
            Iterable<Vertex> friends = bob.getVertices(Vertex.DIRECTION.OUT, "Knows");
            int count = 0;
            for (Vertex friend : friends) {
                count++;
                assertEquals("Charlie", friend.get("name"), "Friend should be Charlie");
            }
            assertEquals(1, count, "Bob should have 1 friend");

            System.out.println("  ✅ EDGE creation and traversal works correctly\n");
        });
    }

    private static void test8_LinkReferenceOperations(Database db) {
        System.out.println("Test 8: LINK reference operations");

        db.transaction(() -> {
            // Create a Person vertex
            MutableVertex dave = db.newVertex("Person");
            dave.set("name", "Dave");
            dave.set("email", "dave@example.com");
            dave.set("age", 35);
            dave.save();

            // Create an Order document with LINK to Person
            MutableDocument order = db.newDocument("Order");
            order.set("orderId", "ORD-001");
            order.set("amount", 99.99);
            order.set("orderDate", "2026-01-14");
            order.set("customerRef", dave.getIdentity());  // LINK reference
            order.save();

            // Retrieve and verify LINK
            Document retrievedOrder = db.select().fromType("Order")
                    .where().property("orderId").eq().value("ORD-001")
                    .documents().next();

            assertNotNull(retrievedOrder, "Order should be retrieved");
            assertNotNull(retrievedOrder.get("customerRef"), "customerRef should exist");
            assertEquals(dave.getIdentity(), retrievedOrder.get("customerRef"), "RID should match");

            System.out.println("  ✅ LINK reference operations work correctly\n");
        });
    }

    private static void test9_CombinedVertexAndLink(Database db) {
        System.out.println("Test 9: Combined VERTEX + LINK");

        db.transaction(() -> {
            // Create two Person vertices
            MutableVertex ceo = db.newVertex("Person");
            ceo.set("name", "Elon");
            ceo.set("email", "elon@spacex.com");
            ceo.set("age", 52);
            ceo.save();

            MutableVertex founder = db.newVertex("Person");
            founder.set("name", "Elon");  // Same person can be both
            founder.set("email", "elon2@spacex.com");  // Different email for unique constraint
            founder.save();

            // Create Company vertex with LINK fields
            MutableVertex company = db.newVertex("Company");
            company.set("name", "SpaceX");
            company.set("industry", "Aerospace");
            company.set("ceoRef", ceo.getIdentity());
            company.set("founderRef", founder.getIdentity());
            company.save();

            // Retrieve and verify
            Vertex retrievedCompany = db.select().fromType("Company")
                    .where().property("name").eq().value("SpaceX")
                    .vertices().next();

            assertNotNull(retrievedCompany, "Company should be retrieved");
            assertEquals("SpaceX", retrievedCompany.get("name"), "Name should match");
            assertEquals(ceo.getIdentity(), retrievedCompany.get("ceoRef"), "CEO RID should match");
            assertEquals(founder.getIdentity(), retrievedCompany.get("founderRef"), "Founder RID should match");

            System.out.println("  ✅ Combined VERTEX + LINK works correctly\n");
        });
    }

    // Helper assertion methods
    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("FAILED: " + message);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FAILED: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("FAILED: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual != null) {
            throw new AssertionError("FAILED: " + message + " (expected null, got " + actual + ")");
        }
        if (expected != null && !expected.equals(actual)) {
            throw new AssertionError("FAILED: " + message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
