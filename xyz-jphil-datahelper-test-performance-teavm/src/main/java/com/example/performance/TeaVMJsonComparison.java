package com.example.performance;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSString;
import org.teavm.jso.JSObject;
import org.teavm.jso.json.JSON;
import xyz.jphil.datahelper.teavm.TeaVMMapLike;

/**
 * TeaVM JSON Performance Comparison
 *
 * Compares three approaches in browser (JavaScript):
 * 1. Native JSON (TeaVM's JSON.parse/stringify - native JavaScript)
 * 2. DataHelper via MapLike (Object → MapLike → JSON)
 * 3. DataHelper Direct (Object → JSON, bypassing MapLike)
 *
 * Similar to JVM tests, but TeaVM-specific with native JSON
 */
public class TeaVMJsonComparison {

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 10000;

    public static void main(String[] args) {
        log("═══════════════════════════════════════════════════════════");
        log("TeaVM JSON Performance Comparison (Running in Browser)");
        log("═══════════════════════════════════════════════════════════\n");

        log("Test Configuration:");
        log("  - Warmup iterations: 10,000");
        log("  - Test iterations: 10,000");
        log("  - Same as JVM tests for fair comparison\n");

        log("Testing three approaches:");
        log("  A) Native JSON (JSON.parse/stringify - native JavaScript)");
        log("  B) DataHelper via MapLike (Object→MapLike→JSON)");
        log("  C) DataHelper Direct (Object→JSON, NO MapLike)\n");

        SimpleDTOTeaVM sample = createSample();

        log("───────────────────────────────────────────────────────────");
        log("Test: Simple DTO (5 fields)");
        log("───────────────────────────────────────────────────────────\n");

        // Scenario A: Native JSON
        log("--- Scenario A: Native JSON (TeaVM built-in) ---");
        testNativeJson(sample);

        // Scenario B: DataHelper via MapLike
        log("\n--- Scenario B: DataHelper via MapLike ---");
        testDataHelperMapLike(sample);

        // Scenario C: DataHelper Direct
        log("\n--- Scenario C: DataHelper Direct (NO MapLike) ---");
        testDataHelperDirect(sample);

        log("\n═══════════════════════════════════════════════════════════");
        log("=== Test Complete ===");
        log("═══════════════════════════════════════════════════════════");
        log("\nInterpretation:");
        log("- Lower ms = FASTER");
        log("- Native JSON uses JavaScript's built-in (highly optimized)");
        log("- DataHelper Direct should be competitive or faster");
    }

    // ========== Scenario A: Native JSON ==========

    private static void testNativeJson(SimpleDTOTeaVM sample) {
        // Convert to JSObject for native JSON
        JSSimpleDTO jsSample = toJSObject(sample);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String json = stringify(jsSample);
            parse(json);
        }

        // Test serialization (Object → JSON String)
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            stringify(jsSample);
        }
        double serTime = now() - start;

        // Test deserialization (JSON String → Object)
        String jsonString = stringify(jsSample);
        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            parse(jsonString);
        }
        double deserTime = now() - start;

        printResult("  Serialization (Object → JSON)", serTime);
        printResult("  Deserialization (JSON → Object)", deserTime);
    }

    // ========== Scenario B: DataHelper via MapLike ==========

    private static void testDataHelperMapLike(SimpleDTOTeaVM sample) {
        // Debug: Check what toMapLike returns
        xyz.jphil.datahelper.MapLike testMapLike = sample.toMapLike(false);
        log("DEBUG: MapLike type: " + testMapLike.getClass().getName());
        JSObject testJsObj = extractJSObject(testMapLike);
        log("DEBUG: Extracted JSObject: " + (testJsObj == null ? "NULL" : testJsObj.toString()));
        log("DEBUG: JSObject type check: " + checkType(testJsObj));

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // Convert to MapLike (JSObject-backed)
            xyz.jphil.datahelper.MapLike mapLike = sample.toMapLike(false);
            // Extract JSObject using helper
            JSObject jsObj = extractJSObject(mapLike);
            // Use native JSON.stringify on the JSObject (FAST!)
            String json = stringify(jsObj);
            // Parse back to JSObject, then wrap in MapLike
            JSObject parsed = parse(json);
            new SimpleDTOTeaVM().fromMapLike(new TeaVMMapLike((JSMapLike<JSObject>) parsed));
        }

        // Test serialization (Object → MapLike → JSON String via native stringify)
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            xyz.jphil.datahelper.MapLike ml = sample.toMapLike(false);
            JSObject jsObj = extractJSObject(ml);
            stringify(jsObj); // Native JSON.stringify on JSObject!
        }
        double serTime = now() - start;

        // Test deserialization (JSON String → MapLike → Object)
        xyz.jphil.datahelper.MapLike tempMapLike = sample.toMapLike(false);
        JSObject tempJsObj = extractJSObject(tempMapLike);
        String jsonString = stringify(tempJsObj);

        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            JSObject parsed = parse(jsonString);
            new SimpleDTOTeaVM().fromMapLike(new TeaVMMapLike((JSMapLike<JSObject>) parsed));
        }
        double deserTime = now() - start;

        printResult("  Serialization (Object → JSON)", serTime);
        printResult("  Deserialization (JSON → Object)", deserTime);
    }

    /**
     * Extract the underlying JSObject from a MapLike.
     * Uses JSBody to directly access the internal jsMap field.
     * Note: TeaVM mangles field names - jsMap becomes $jsMap
     */
    @JSBody(params = {"mapLike"}, script = "return mapLike.$jsMap;")
    private static native JSObject extractJSObject(xyz.jphil.datahelper.MapLike mapLike);

    /**
     * Debug: Check the type of a JSObject
     */
    @JSBody(params = {"obj"}, script = "return typeof obj + ' / ' + (obj === null ? 'null' : obj === undefined ? 'undefined' : 'defined');")
    private static native String checkType(JSObject obj);

    // ========== Scenario C: DataHelper Direct ==========

    private static void testDataHelperDirect(SimpleDTOTeaVM sample) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String json = TeaVMDirectJsonWriter.toJson(sample);
            TeaVMSimpleJsonReader.fromJson(json);
        }

        // Test serialization (Object → JSON String directly)
        double start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            TeaVMDirectJsonWriter.toJson(sample);
        }
        double serTime = now() - start;

        // Test deserialization (JSON String → Object directly)
        String jsonString = TeaVMDirectJsonWriter.toJson(sample);
        start = now();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            TeaVMSimpleJsonReader.fromJsonToSimpleDTO(jsonString);
        }
        double deserTime = now() - start;

        printResult("  Serialization (Object → JSON)", serTime);
        printResult("  Deserialization (JSON → Object)", deserTime);
    }

    // ========== Helper Methods ==========

    private static SimpleDTOTeaVM createSample() {
        SimpleDTOTeaVM dto = new SimpleDTOTeaVM();
        dto.setName("Test User");
        dto.setAge(30);
        dto.setEmail("test@example.com");
        dto.setStatus(1);
        dto.setSalary(80000.0);
        return dto;
    }

    /**
     * Convert SimpleDTOTeaVM to JSObject for native JSON.
     * This is what you'd do in a typical TeaVM app using @JSProperty.
     */
    @JSBody(params = {"name", "age", "email", "status", "salary"},
            script = "return {name: name, age: age, email: email, status: status, salary: salary};")
    private static native JSSimpleDTO createJSObject(String name, int age, String email, int status, double salary);

    private static JSSimpleDTO toJSObject(SimpleDTOTeaVM dto) {
        return createJSObject(dto.getName(), dto.getAge(), dto.getEmail(), dto.getStatus(), dto.getSalary());
    }

    private static void printResult(String label, double timeMs) {
        double opsPerSec = (TEST_ITERATIONS / timeMs) * 1000.0;
        log(String.format("  %-35s: %7.2f ms (%,.0f ops/sec)", label, timeMs, opsPerSec));
    }

    @JSBody(params = {"msg"}, script = "console.log(msg);")
    private static native void log(String msg);

    @JSBody(script = "return performance.now();")
    private static native double now();

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

    /**
     * Stringify a Java Map - simple manual JSON builder
     */
    private static String stringifyMap(java.util.Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("null"); // Simplified - just handle primitives
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
