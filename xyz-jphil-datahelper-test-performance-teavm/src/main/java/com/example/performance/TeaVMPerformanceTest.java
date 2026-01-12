package com.example.performance;

import org.teavm.jso.JSBody;
import xyz.jphil.datahelper.teavm.TeaVMMapLike;

import java.util.*;

/**
 * TeaVM Performance Test - Measures DataHelper performance in JavaScript
 *
 * This test compiles to JavaScript and runs natively via GraalJS.
 * Measures:
 * 1. Serialization performance (Object -> MapLike)
 * 2. Deserialization performance (MapLike -> Object)
 * 3. Throughput (operations per second)
 *
 * Compile: mvn compile
 * Run: cd .. && mvn -f xyz-jphil-datahelper-test-performance-teavm/pom.xml exec:java -Dexec.mainClass="com.example.performance.GraalJSRunner"
 */
public class TeaVMPerformanceTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;

    public static void main(String[] args) {
        log("=== TeaVM Performance Test (JavaScript) ===\n");

        // Test 1: Simple DTO
        log("Test 1: Simple DTO (5 fields)");
        testSimpleDTO();

        // Test 2: Complex DTO
        log("\nTest 2: Complex DTO (nested objects, lists, maps)");
        testComplexDTO();

        // Test 3: Large Lists
        log("\nTest 3: Large List (1000 items)");
        testLargeList();

        log("\n=== Test Complete ===");
    }

    // ========== Simple DTO Tests ==========

    private static void testSimpleDTO() {
        SimpleDTOTeaVM sample = createSimpleDTO("Test User", 30, "test@example.com");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            sample.toMapLike(false);
            TeaVMMapLike mapLike = (TeaVMMapLike) sample.toMapLike(false);
            new SimpleDTOTeaVM().fromMapLike(mapLike);
        }

        // Test serialization
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            sample.toMapLike(false);
        }
        double serTime = now() - start;

        // Test deserialization
        TeaVMMapLike mapLike = (TeaVMMapLike) sample.toMapLike(false);
        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            new SimpleDTOTeaVM().fromMapLike(mapLike);
        }
        double deserTime = now() - start;

        printResult("  Serialization", serTime, TEST_ITERATIONS);
        printResult("  Deserialization", deserTime, TEST_ITERATIONS);
    }

    // ========== Complex DTO Tests ==========

    private static void testComplexDTO() {
        ComplexDTOTeaVM sample = createComplexDTO();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            sample.toMapLike(true);
            TeaVMMapLike mapLike = (TeaVMMapLike) sample.toMapLike(true);
            new ComplexDTOTeaVM().fromMapLike(mapLike);
        }

        // Test serialization (deep)
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            sample.toMapLike(true);
        }
        double serTime = now() - start;

        // Test deserialization
        TeaVMMapLike mapLike = (TeaVMMapLike) sample.toMapLike(true);
        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            new ComplexDTOTeaVM().fromMapLike(mapLike);
        }
        double deserTime = now() - start;

        printResult("  Serialization", serTime, TEST_ITERATIONS);
        printResult("  Deserialization", deserTime, TEST_ITERATIONS);
    }

    // ========== Large List Tests ==========

    private static void testLargeList() {
        List<SimpleDTOTeaVM> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(createSimpleDTO("User " + i, 20 + (i % 50), "user" + i + "@example.com"));
        }

        // Warmup
        for (int i = 0; i < 100; i++) {
            serializeList(largeList);
            List<TeaVMMapLike> mapLikes = serializeList(largeList);
            deserializeList(mapLikes);
        }

        // Test serialization
        double start = now();
        for (int i = 0; i < 1000; i++) {
            serializeList(largeList);
        }
        double serTime = now() - start;

        // Test deserialization
        List<TeaVMMapLike> mapLikes = serializeList(largeList);
        start = now();
        for (int i = 0; i < 1000; i++) {
            deserializeList(mapLikes);
        }
        double deserTime = now() - start;

        printResult("  Serialization (1000 items)", serTime, 1000);
        printResult("  Deserialization (1000 items)", deserTime, 1000);
    }

    // ========== Helper Methods ==========

    private static SimpleDTOTeaVM createSimpleDTO(String name, int age, String email) {
        SimpleDTOTeaVM dto = new SimpleDTOTeaVM();
        dto.setName(name);
        dto.setAge(age);
        dto.setEmail(email);
        dto.setStatus(1); // 1=active, 0=inactive
        dto.setSalary(50000.0 + age * 1000);
        return dto;
    }

    private static ComplexDTOTeaVM createComplexDTO() {
        ComplexDTOTeaVM dto = new ComplexDTOTeaVM();
        dto.setId("complex-001");
        dto.setName("Complex Test");
        dto.setVersion(1);
        dto.setStatus(1); // 1=enabled, 0=disabled

        // Nested object
        dto.setMetadata(createSimpleDTO("Metadata", 25, "meta@example.com"));

        // List of strings
        dto.setTags(Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5"));

        // List of objects
        List<SimpleDTOTeaVM> relatedItems = new ArrayList<>();
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

    private static List<TeaVMMapLike> serializeList(List<SimpleDTOTeaVM> list) {
        List<TeaVMMapLike> result = new ArrayList<>();
        for (SimpleDTOTeaVM dto : list) {
            result.add((TeaVMMapLike) dto.toMapLike(false));
        }
        return result;
    }

    private static List<SimpleDTOTeaVM> deserializeList(List<TeaVMMapLike> mapLikes) {
        List<SimpleDTOTeaVM> result = new ArrayList<>();
        for (TeaVMMapLike mapLike : mapLikes) {
            result.add(new SimpleDTOTeaVM().fromMapLike(mapLike));
        }
        return result;
    }

    private static void printResult(String label, double timeMs, int iterations) {
        double opsPerSec = (iterations / timeMs) * 1000.0;
        log(String.format("  %-30s: %7.2f ms (%,.0f ops/sec)", label, timeMs, opsPerSec));
    }

    @JSBody(params = {"msg"}, script = "console.log(msg);")
    private static native void log(String msg);

    @JSBody(script = "return performance.now();")
    private static native double now();
}
