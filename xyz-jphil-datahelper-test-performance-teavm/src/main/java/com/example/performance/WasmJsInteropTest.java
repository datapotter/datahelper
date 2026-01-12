package com.example.performance;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.json.JSON;

/**
 * Test WASM calling JavaScript native JSON methods
 * Measures the overhead of WASM ↔ JavaScript boundary crossing
 */
public class WasmJsInteropTest {

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 10000;

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("WASM ↔ JavaScript Interop Performance Test");
        System.out.println("===================================================================\n");

        System.out.println("Test Configuration:");
        System.out.println("  - Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("  - Test iterations: " + TEST_ITERATIONS + "\n");

        System.out.println("Tests:");
        System.out.println("  A) Pure WASM - DataHelper (measured earlier)");
        System.out.println("  B) WASM → JS Native JSON → WASM (this test)");
        System.out.println("  C) Pure JS Native JSON (measured earlier)\n");

        SimpleDTOTeaVM sample = createSample();

        System.out.println("-------------------------------------------------------------------");
        System.out.println("Test 1: WASM Calling Native JSON (with boundary crossing)");
        System.out.println("-------------------------------------------------------------------\n");

        testNativeJsonFromWasm(sample);

        System.out.println("\n===================================================================");
        System.out.println("=== Summary ===");
        System.out.println("===================================================================\n");

        System.out.println("Expected Results:");
        System.out.println("  Pure JS Native JSON:      ~2ms ser,   ~4ms deser   (fastest)");
        System.out.println("  WASM → JS Native JSON:     ???ms ser,  ???ms deser  (+ boundary cost)");
        System.out.println("  Pure WASM DataHelper:    570ms ser, 439ms deser   (slowest)");
        System.out.println();
        System.out.println("Boundary crossing overhead = (B - C)");
        System.out.println();
    }

    private static void testNativeJsonFromWasm(SimpleDTOTeaVM sample) {
        System.out.println("--- WASM calling JavaScript's native JSON ---");

        // Create a JavaScript object from our DTO
        // In WASM, we need to manually construct the JS object structure
        JSObject jsObj = createJSObject(
            sample.getName(),
            sample.getAge(),
            sample.getEmail(),
            sample.getStatus(),
            sample.getSalary()
        );

        // Warmup
        System.out.println("Warming up (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String json = stringify(jsObj);
            parse(json);
        }
        System.out.println("Warmup complete.\n");

        // Test serialization (JSObject → JSON String via native JSON.stringify)
        long start = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            stringify(jsObj);
        }
        long serTime = System.currentTimeMillis() - start;

        // Test deserialization (JSON String → JSObject via native JSON.parse)
        String jsonString = stringify(jsObj);
        start = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            parse(jsonString);
        }
        long deserTime = System.currentTimeMillis() - start;

        printResult("  Serialization (JSObject → JSON)", serTime);
        printResult("  Deserialization (JSON → JSObject)", deserTime);

        // Comparison
        System.out.println("\nComparison:");
        System.out.printf("  Pure JS Native JSON:         ~2ms ser,   ~4ms deser   (measured earlier)\n");
        System.out.printf("  WASM → JS Native JSON:     %4.0fms ser, %4.0fms deser  (this test)\n",
            (double)serTime, (double)deserTime);
        System.out.printf("  Pure WASM DataHelper:      570ms ser, 439ms deser   (measured earlier)\n\n");

        double boundaryCostSer = serTime - 2.0;
        double boundaryCostDeser = deserTime - 4.0;

        System.out.printf("Estimated Boundary Crossing Overhead:\n");
        System.out.printf("  Serialization:   ~%.0fms (%.0f%% of native JSON time)\n",
            boundaryCostSer, (boundaryCostSer / 2.0) * 100);
        System.out.printf("  Deserialization: ~%.0fms (%.0f%% of native JSON time)\n",
            boundaryCostDeser, (boundaryCostDeser / 4.0) * 100);
    }

    /**
     * Create a JavaScript object with the DTO fields
     */
    @JSBody(params = {"name", "age", "email", "status", "salary"}, script = """
        return {
            name: name,
            age: age,
            email: email,
            status: status,
            salary: salary
        };
    """)
    private static native JSObject createJSObject(String name, int age, String email, int status, double salary);

    /**
     * Call native JSON.stringify
     */
    @JSBody(params = {"obj"}, script = "return JSON.stringify(obj);")
    private static native String stringify(JSObject obj);

    /**
     * Call native JSON.parse
     */
    @JSBody(params = {"json"}, script = "return JSON.parse(json);")
    private static native JSObject parse(String json);

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
        System.out.printf("  %-40s: %7.2f ms (%,.0f ops/sec)\n", label, (double)timeMs, opsPerSec);
    }
}
