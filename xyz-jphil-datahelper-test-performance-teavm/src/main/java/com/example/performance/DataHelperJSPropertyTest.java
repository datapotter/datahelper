package com.example.performance;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.json.JSON;

/**
 * Test DataHelper-generated @JSProperty interfaces with native JSON.
 * This is the ACTUAL production use case - not custom serialization!
 *
 * DataHelper generates JSObject-compatible interfaces, so you can:
 * 1. Create DTOs as regular Java objects (SimpleDTOTeaVM)
 * 2. Cast to JSObject for native JSON
 * 3. Use native JSON.stringify/parse (C++ optimized)
 */
public class DataHelperJSPropertyTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 5000;

    public static void main(String[] args) {
        log("═══════════════════════════════════════════════════════════");
        log("DataHelper @JSProperty + Native JSON Test");
        log("═══════════════════════════════════════════════════════════\n");

        log("Testing the ACTUAL production use case:");
        log("1. DataHelper generates @JSProperty interfaces");
        log("2. DTOs implement these interfaces (JSObject-compatible)");
        log("3. Cast DTO to JSObject");
        log("4. Use native JSON.stringify/parse (browser's C++ implementation)\n");

        log("───────────────────────────────────────────────────────────");
        log("Test: Simple DTO (5 fields)");
        log("───────────────────────────────────────────────────────────\n");

        // Create DTO using DataHelper-generated interface
        SimpleDTOTeaVM dto = new SimpleDTOTeaVM();
        dto.setName("Test User");
        dto.setAge(30);
        dto.setEmail("test@example.com");
        dto.setStatus(1);
        dto.setSalary(80000.0);

        log("--- DataHelper @JSProperty DTO + Native JSON ---");
        testDataHelperWithNativeJSON(dto);

        log("\n═══════════════════════════════════════════════════════════");
        log("=== Test Complete ===");
        log("═══════════════════════════════════════════════════════════");
        log("\nKey Insight:");
        log("- DataHelper GENERATES @JSProperty interfaces for you");
        log("- Your DTO implements JSObject-compatible interface");
        log("- Native JSON works directly on your DTO (cast to JSObject)");
        log("- NO custom serialization needed!");
        log("- NO manual Map conversion needed!");
        log("\nThis is the power of DataHelper's annotation processor:");
        log("  Generate once, use with native JSON everywhere!");
    }

    private static void testDataHelperWithNativeJSON(SimpleDTOTeaVM dto) {
        // Cast DTO to JSObject (safe because it implements @JSProperty interface)
        JSObject jsObj = castToJSObject(dto);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String json = stringify(jsObj);
            parse(json);
        }

        // Test serialization (Object → JSON String)
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            stringify(jsObj);
        }
        double serTime = now() - start;

        // Test deserialization (JSON String → Object)
        String jsonString = stringify(jsObj);
        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            SimpleDTOTeaVM_I<?> parsed = (SimpleDTOTeaVM_I<?>) parse(jsonString);
            // Can access properties: parsed.getName(), parsed.getAge(), etc.
        }
        double deserTime = now() - start;

        printResult("  Serialization (Object → JSON)", serTime);
        printResult("  Deserialization (JSON → Object)", deserTime);

        log("\n  Properties accessible via @JSProperty interface:");
        SimpleDTOTeaVM_I<?> parsed = (SimpleDTOTeaVM_I<?>) parse(jsonString);
        log("    - Name: " + parsed.getName());
        log("    - Age: " + parsed.getAge());
        log("    - Email: " + parsed.getEmail());
        log("    - Status: " + parsed.getStatus());
        log("    - Salary: " + parsed.getSalary());
    }

    // ========== Helper Methods ==========

    private static void printResult(String label, double timeMs) {
        double opsPerSec = (TEST_ITERATIONS / timeMs) * 1000.0;
        log(String.format("  %-35s: %7.2f ms (%,.0f ops/sec)", label, timeMs, opsPerSec));
    }

    @JSBody(params = {"msg"}, script = "console.log(msg);")
    private static native void log(String msg);

    @JSBody(script = "return performance.now();")
    private static native double now();

    /**
     * Cast Java object to JSObject (safe for @JSProperty DTOs)
     */
    @JSBody(params = {"obj"}, script = "return obj;")
    private static native JSObject castToJSObject(Object obj);

    /**
     * Wrapper for JSON.stringify that returns Java String
     */
    @JSBody(params = {"obj"}, script = "return JSON.stringify(obj);")
    private static native String stringify(JSObject obj);

    /**
     * Wrapper for JSON.parse that accepts Java String
     */
    @JSBody(params = {"json"}, script = "return JSON.parse(json);")
    private static native JSObject parse(String json);
}
