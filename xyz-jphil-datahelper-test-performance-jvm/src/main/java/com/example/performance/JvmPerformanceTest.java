package com.example.performance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * JVM Performance Test - Compares DataHelper with Jackson
 *
 * This test runs on standard JVM and measures:
 * 1. Serialization performance (Object -> Map)
 * 2. Deserialization performance (Map -> Object)
 * 3. Throughput (operations per second)
 *
 * Run: cd xyz-jphil-datahelper-test-performance-jvm && mvn clean compile exec:java -Dexec.mainClass="com.example.performance.JvmPerformanceTest"
 */
public class JvmPerformanceTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.out.println("=== JVM Performance Test: DataHelper vs Jackson ===\n");
        System.out.println("Testing four scenarios:");
        System.out.println("  A) Object ↔ Map (DataHelper's core use case)");
        System.out.println("  B) Object ↔ JSON using Jackson for JSON conversion");
        System.out.println("  C) Object ↔ JSON using custom JSON converter (Object→Map→JSON)");
        System.out.println("  D) Object ↔ JSON DIRECT (Object→JSON, bypassing Map!)\n");

        // Test 1: Simple DTO
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Test 1: Simple DTO (5 fields)");
        System.out.println("═══════════════════════════════════════════════════");
        testSimpleDTO();

        // Test 2: Complex DTO
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("Test 2: Complex DTO (nested objects, lists, maps)");
        System.out.println("═══════════════════════════════════════════════════");
        testComplexDTO();

        // Test 3: Large Lists
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("Test 3: Large List (1000 items)");
        System.out.println("═══════════════════════════════════════════════════");
        testLargeList();

        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("=== Test Complete ===");
        System.out.println("═══════════════════════════════════════════════════");
    }

    // ========== Simple DTO Tests ==========

    private static void testSimpleDTO() throws Exception {
        SimpleDTO sample = createSimpleDTO("Test User", 30, "test@example.com");

        System.out.println("\n--- Scenario A: Object ↔ Map ---");
        testSimpleDTO_ObjectToMap(sample);

        System.out.println("\n--- Scenario B: Object ↔ JSON (with Jackson JSON) ---");
        testSimpleDTO_ObjectToJsonJackson(sample);

        System.out.println("\n--- Scenario C: Object ↔ JSON (with Custom JSON) ---");
        testSimpleDTO_ObjectToJsonCustom(sample);

        System.out.println("\n--- Scenario D: Object ↔ JSON (Direct - NO Map!) ---");
        testSimpleDTO_ObjectToJsonDirect(sample);
    }

    private static void testSimpleDTO_ObjectToMap(SimpleDTO sample) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            sample.toMap(false);
            Map<String, Object> map = sample.toMap(false);
            new SimpleDTO().fromMap(map);
            objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        }

        // DataHelper: Object -> Map
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            sample.toMap(false);
        }
        long dhTime = System.nanoTime() - start;

        // Jackson: Object -> Map
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (Object → Map)", dhTime, jTime);

        // DataHelper: Map -> Object
        Map<String, Object> map = sample.toMap(false);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            new SimpleDTO().fromMap(map);
        }
        dhTime = System.nanoTime() - start;

        // Jackson: Map -> Object
        Map<String, Object> jMap = objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.convertValue(jMap, SimpleDTO.class);
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (Map → Object)", dhTime, jTime);
    }

    private static void testSimpleDTO_ObjectToJsonJackson(SimpleDTO sample) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Map<String, Object> map = sample.toMap(false);
            objectMapper.writeValueAsString(map);
            String json = objectMapper.writeValueAsString(sample);
            objectMapper.readValue(json, SimpleDTO.class);
        }

        // DataHelper+Jackson: Object -> Map -> JSON
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> map = sample.toMap(false);
            objectMapper.writeValueAsString(map);
        }
        long dhTime = System.nanoTime() - start;

        // Pure Jackson: Object -> JSON
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.writeValueAsString(sample);
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (Object → JSON)", dhTime, jTime);

        // DataHelper+Jackson: JSON -> Map -> Object
        Map<String, Object> map = sample.toMap(false);
        String jsonString = objectMapper.writeValueAsString(map);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> m = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            new SimpleDTO().fromMap(m);
        }
        dhTime = System.nanoTime() - start;

        // Pure Jackson: JSON -> Object
        String jJson = objectMapper.writeValueAsString(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.readValue(jJson, SimpleDTO.class);
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (JSON → Object)", dhTime, jTime);
    }

    private static void testSimpleDTO_ObjectToJsonCustom(SimpleDTO sample) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Map<String, Object> map = sample.toMap(false);
            SimpleJsonWriter.toJson(map);
            String json = SimpleJsonWriter.toJson(map);
            SimpleJsonReader.fromJson(json);
        }

        // DataHelper+Custom: Object -> Map -> JSON
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> map = sample.toMap(false);
            SimpleJsonWriter.toJson(map);
        }
        long dhTime = System.nanoTime() - start;

        // Pure Jackson: Object -> JSON
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.writeValueAsString(sample);
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (Object → JSON)", dhTime, jTime);

        // DataHelper+Custom: JSON -> Map -> Object
        Map<String, Object> map = sample.toMap(false);
        String jsonString = SimpleJsonWriter.toJson(map);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> m = SimpleJsonReader.fromJson(jsonString);
            new SimpleDTO().fromMap(m);
        }
        dhTime = System.nanoTime() - start;

        // Pure Jackson: JSON -> Object
        String jJson = objectMapper.writeValueAsString(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.readValue(jJson, SimpleDTO.class);
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (JSON → Object)", dhTime, jTime);
    }

    private static void testSimpleDTO_ObjectToJsonDirect(SimpleDTO sample) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            DirectJsonWriter.toJson(sample);
            String json = DirectJsonWriter.toJson(sample);
            SimpleJsonReader.fromJson(json);
        }

        // DataHelper Direct: Object -> JSON (NO Map intermediate!)
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DirectJsonWriter.toJson(sample);
        }
        long dhTime = System.nanoTime() - start;

        // Pure Jackson: Object -> JSON
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.writeValueAsString(sample);
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (Object → JSON)", dhTime, jTime);

        // DataHelper Direct: JSON -> Map -> Object (deserialization still uses Map)
        String jsonString = DirectJsonWriter.toJson(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> m = SimpleJsonReader.fromJson(jsonString);
            new SimpleDTO().fromMap(m);
        }
        dhTime = System.nanoTime() - start;

        // Pure Jackson: JSON -> Object
        String jJson = objectMapper.writeValueAsString(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.readValue(jJson, SimpleDTO.class);
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (JSON → Object)", dhTime, jTime);
    }

    // ========== Complex DTO Tests ==========

    private static void testComplexDTO() throws Exception {
        ComplexDTO sample = createComplexDTO();

        System.out.println("\n--- Scenario A: Object ↔ Map (deep=true) ---");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            sample.toMap(true);
            Map<String, Object> map = sample.toMap(true);
            new ComplexDTO().fromMap(map);
            objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        }

        // DataHelper: Object -> Map (deep)
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            sample.toMap(true);
        }
        long dhTime = System.nanoTime() - start;

        // Jackson: Object -> Map
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (Object → Map)", dhTime, jTime);

        // DataHelper: Map -> Object
        Map<String, Object> map = sample.toMap(true);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            new ComplexDTO().fromMap(map);
        }
        dhTime = System.nanoTime() - start;

        // Jackson: Map -> Object
        Map<String, Object> jMap = objectMapper.convertValue(sample, new TypeReference<Map<String, Object>>() {});
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.convertValue(jMap, ComplexDTO.class);
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (Map → Object)", dhTime, jTime);
    }

    // ========== Large List Tests ==========

    private static void testLargeList() throws Exception {
        List<SimpleDTO> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(createSimpleDTO("User " + i, 20 + (i % 50), "user" + i + "@example.com"));
        }

        System.out.println("\n--- Scenario A: List ↔ List<Map> ---");

        // Warmup
        for (int i = 0; i < 100; i++) {
            serializeList(largeList);
            List<Map<String, Object>> maps = serializeList(largeList);
            deserializeList(maps);
            objectMapper.convertValue(largeList, new TypeReference<List<Map<String, Object>>>() {});
        }

        // DataHelper: List -> List<Map>
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            serializeList(largeList);
        }
        long dhTime = System.nanoTime() - start;

        // Jackson: List -> List<Map>
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            objectMapper.convertValue(largeList, new TypeReference<List<Map<String, Object>>>() {});
        }
        long jTime = System.nanoTime() - start;

        printResults("  Serialization (List → List<Map>)", dhTime, jTime);

        // DataHelper: List<Map> -> List
        List<Map<String, Object>> maps = serializeList(largeList);
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            deserializeList(maps);
        }
        dhTime = System.nanoTime() - start;

        // Jackson: List<Map> -> List
        List<Map<String, Object>> jMaps = objectMapper.convertValue(largeList, new TypeReference<List<Map<String, Object>>>() {});
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            objectMapper.convertValue(jMaps, new TypeReference<List<SimpleDTO>>() {});
        }
        jTime = System.nanoTime() - start;

        printResults("  Deserialization (List<Map> → List)", dhTime, jTime);
    }

    // ========== Helper Methods ==========

    private static SimpleDTO createSimpleDTO(String name, int age, String email) {
        SimpleDTO dto = new SimpleDTO();
        dto.setName(name);
        dto.setAge(age);
        dto.setEmail(email);
        dto.setStatus(1); // 1=active, 0=inactive
        dto.setSalary(50000.0 + age * 1000);
        return dto;
    }

    private static ComplexDTO createComplexDTO() {
        ComplexDTO dto = new ComplexDTO();
        dto.setId("complex-001");
        dto.setName("Complex Test");
        dto.setVersion(1);
        dto.setStatus(1); // 1=enabled, 0=disabled

        // Nested object
        dto.setMetadata(createSimpleDTO("Metadata", 25, "meta@example.com"));

        // List of strings
        dto.setTags(Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5"));

        // List of objects
        List<SimpleDTO> relatedItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            relatedItems.add(createSimpleDTO("Related " + i, 20 + i, "related" + i + "@example.com"));
        }
        dto.setRelatedItems(relatedItems);

        // Map with string values
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        properties.put("key3", "value3");
        dto.setProperties(properties);

        // Map with integer values
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("math", 95);
        scores.put("science", 87);
        scores.put("english", 92);
        dto.setScores(scores);

        return dto;
    }

    private static List<Map<String, Object>> serializeList(List<SimpleDTO> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SimpleDTO dto : list) {
            result.add(dto.toMap(false));
        }
        return result;
    }

    private static List<SimpleDTO> deserializeList(List<Map<String, Object>> maps) {
        List<SimpleDTO> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            result.add(new SimpleDTO().fromMap(map));
        }
        return result;
    }

    private static void printResults(String testName, long dataHelperNanos, long jacksonNanos) {
        double dataHelperMs = dataHelperNanos / 1_000_000.0;
        double jacksonMs = jacksonNanos / 1_000_000.0;
        double ratio = (double) dataHelperNanos / jacksonNanos;
        String performance = ratio < 1.0 ? "FASTER" : "SLOWER";

        System.out.printf("  %-40s: DataHelper=%7.2f ms, Jackson=%7.2f ms (%.2fx %s)\n",
                testName, dataHelperMs, jacksonMs, Math.abs(ratio), performance);
    }
}
