package com.example.performance;

/**
 * WebAssembly Performance Test - Pure Java only
 *
 * Tests ONLY Scenario C (DataHelper Direct) because:
 * - Scenario A (Native JSON) requires @JSBody (JS-only)
 * - Scenario B (MapLike) requires @JSBody (JS-only)
 * - Scenario C (Pure Java) works in both JS and WASM
 *
 * This compares:
 * - JavaScript compilation (from earlier test): 152.10ms ser, 86.40ms deser
 * - WebAssembly compilation (this test): ???ms
 */
public class WasmPerformanceTest {

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 10000;

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("WebAssembly Performance Test - DataHelper Direct");
        System.out.println("===================================================================\n");

        System.out.println("Test Configuration:");
        System.out.println("  - Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("  - Test iterations: " + TEST_ITERATIONS);
        System.out.println("  - Testing DataHelper Direct (Pure Java)\n");

        System.out.println("Comparison:");
        System.out.println("  - JavaScript (previous): 152.10ms ser, 86.40ms deser");
        System.out.println("  - WebAssembly (this test): ???ms\n");

        // Sanity check: simple string operations
        System.out.println(">>> Sanity Check: Simple StringBuilder Test <<<");
        testStringBuilder();
        System.out.println();

        SimpleDTOTeaVM sample = createSample();

        System.out.println("-------------------------------------------------------------------");
        System.out.println("Test: Simple DTO (5 fields)");
        System.out.println("-------------------------------------------------------------------\n");

        testDataHelperDirect(sample);

        System.out.println("\n===================================================================");
        System.out.println("=== Test Complete ===");
        System.out.println("===================================================================");
    }

    private static void testStringBuilder() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"name\":\"test\",\"value\":").append(i).append("}");
            sb.toString();
        }

        // Test
        long start = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"name\":\"test\",\"value\":").append(i).append("}");
            sb.toString();
        }
        long time = System.currentTimeMillis() - start;

        System.out.printf("  Simple StringBuilder test: %7.2f ms (%,.0f ops/sec)\n",
            (double)time, (TEST_ITERATIONS / (double)time) * 1000.0);
        System.out.println("  (This should be fast - if slow, WASM itself might be slow)");
    }

    private static void testDataHelperDirect(SimpleDTOTeaVM sample) {
        System.out.println("--- DataHelper Direct (Pure Java, NO native calls) ---");

        // Warmup
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String json = TeaVMDirectJsonWriter.toJson(sample);
            TeaVMSimpleJsonReader.fromJson(json);
        }
        System.out.println("Warmup complete.\n");

        // Test serialization (Object → JSON String directly)
        long start = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            TeaVMDirectJsonWriter.toJson(sample);
        }
        long serTime = System.currentTimeMillis() - start;

        // Test deserialization (JSON String → Object directly)
        String jsonString = TeaVMDirectJsonWriter.toJson(sample);
        start = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            TeaVMSimpleJsonReader.fromJsonToSimpleDTO(jsonString);
        }
        long deserTime = System.currentTimeMillis() - start;

        printResult("  Serialization (Object → JSON)", serTime);
        printResult("  Deserialization (JSON → Object)", deserTime);

        // Comparison with JavaScript
        System.out.println("\nComparison with JavaScript:");
        double jsSerTime = 152.10;
        double jsDeserTime = 86.40;

        double serRatio = jsSerTime / serTime;
        double deserRatio = jsDeserTime / deserTime;

        System.out.printf("  Serialization:   WASM %6.2fms vs JS %6.2fms (%.2fx %s)\n",
            (double)serTime, jsSerTime,
            Math.abs(serRatio),
            serRatio > 1.0 ? "FASTER" : "SLOWER");
        System.out.printf("  Deserialization: WASM %6.2fms vs JS %6.2fms (%.2fx %s)\n",
            (double)deserTime, jsDeserTime,
            Math.abs(deserRatio),
            deserRatio > 1.0 ? "FASTER" : "SLOWER");
    }

    private static SimpleDTOTeaVM createSample() {
        SimpleDTOTeaVM dto = new SimpleDTOTeaVM();
        dto.setName("Test User");
        dto.setAge(30);
        dto.setEmail("test@example.com");
        dto.setStatus(1);
        dto.setSalary(80000.0);
        return dto;
    }

    private static void printResult(String label, long timeMs) {
        double opsPerSec = (TEST_ITERATIONS / (double)timeMs) * 1000.0;
        System.out.printf("  %-35s: %7.2f ms (%,.0f ops/sec)\n", label, (double)timeMs, opsPerSec);
    }
}
