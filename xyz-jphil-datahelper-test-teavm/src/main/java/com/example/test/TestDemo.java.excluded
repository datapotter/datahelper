package com.example.test;

import java.util.List;
import java.util.Map;

import static com.example.test.EmailBatch_I.*;

public class TestDemo {
    public static void main(String[] args) {
        System.out.println("=== Testing GenDTOSymbolsInterface ===\n");

        // Test fluent API
        System.out.println("1. Creating EmailBatch with fluent API:");
        EmailBatch batch = new EmailBatch()
                .batchId("BATCH-123")
                .entityName("example.com")
                .urlCount(42)
                .trackingIds(List.of("id1", "id2", "id3"));

        System.out.println("   BatchId: " + batch.batchId());
        System.out.println("   EntityName: " + batch.entityName());
        System.out.println("   UrlCount: " + batch.urlCount());
        System.out.println("   TrackingIds: " + batch.trackingIds());

        // Test verbose API
        System.out.println("\n2. Testing verbose getters/setters:");
        batch.setBatchId("BATCH-456");
        System.out.println("   Updated BatchId: " + batch.getBatchId());

        // Test field symbols
        System.out.println("\n3. Testing field symbols:");
        System.out.println("   $batchId = " + $batchId);
        System.out.println("   $entityName = " + $entityName);
        System.out.println("   $urlCount = " + $urlCount);
        System.out.println("   $trackingIds = " + $trackingIds);

        // Test FIELDS list
        System.out.println("\n4. All fields:");
        System.out.println("   FIELDS = " + FIELDS);

        // Test toMap()
        System.out.println("\n5. Testing toMap():");
        Map<String, Object> map = batch.toMap();
        System.out.println("   Map: " + map);

        // Test fromMap()
        System.out.println("\n6. Testing fromMap():");
        EmailBatch batch2 = new EmailBatch()
                .fromMap(Map.of(
                        $batchId, "BATCH-789",
                        $entityName, "test.com",
                        $urlCount, 100
                ));
        System.out.println("   New batch from map:");
        System.out.println("   BatchId: " + batch2.batchId());
        System.out.println("   EntityName: " + batch2.entityName());
        System.out.println("   UrlCount: " + batch2.urlCount());

        System.out.println("\n=== All tests completed successfully! ===");
        batch2.getBatchId();
        batch2.batchId();
    }
}
