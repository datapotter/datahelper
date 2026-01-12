package com.example.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Test ONLY Scenario D with proper warmup to isolate JIT effects
 */
public class ScenarioDOnly {

    private static final int WARMUP_ITERATIONS = 10000; // Much more warmup!
    private static final int TEST_ITERATIONS = 10000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Scenario D Only - Fair JIT Warmup Test ===\n");

        SimpleDTO sample = createSimpleDTO("Test User", 30, "test@example.com");

        System.out.println("Warmup phase: " + WARMUP_ITERATIONS + " iterations...");

        // Warmup BOTH approaches equally
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // DataHelper Direct
            DirectJsonWriter.toJson(sample);
            String json1 = DirectJsonWriter.toJson(sample);
            SimpleJsonReader.fromJson(json1);

            // Jackson
            objectMapper.writeValueAsString(sample);
            String json2 = objectMapper.writeValueAsString(sample);
            objectMapper.readValue(json2, SimpleDTO.class);
        }

        System.out.println("Warmup complete. Running tests...\n");

        // Test 1: Serialization
        long start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DirectJsonWriter.toJson(sample);
        }
        long dhTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.writeValueAsString(sample);
        }
        long jTime = System.nanoTime() - start;

        double dhMs = dhTime / 1_000_000.0;
        double jMs = jTime / 1_000_000.0;
        double ratio = dhMs / jMs;

        System.out.printf("Serialization (Object → JSON):\n");
        System.out.printf("  DataHelper Direct: %6.2f ms\n", dhMs);
        System.out.printf("  Jackson:          %6.2f ms\n", jMs);
        System.out.printf("  Ratio:            %6.2fx %s\n\n", ratio, ratio < 1.0 ? "FASTER" : "SLOWER");

        // Test 2: Deserialization
        String jsonString = DirectJsonWriter.toJson(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> m = SimpleJsonReader.fromJson(jsonString);
            new SimpleDTO().fromMap(m);
        }
        dhTime = System.nanoTime() - start;

        String jJson = objectMapper.writeValueAsString(sample);
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            objectMapper.readValue(jJson, SimpleDTO.class);
        }
        jTime = System.nanoTime() - start;

        dhMs = dhTime / 1_000_000.0;
        jMs = jTime / 1_000_000.0;
        ratio = dhMs / jMs;

        System.out.printf("Deserialization (JSON → Object):\n");
        System.out.printf("  DataHelper Direct: %6.2f ms\n", dhMs);
        System.out.printf("  Jackson:          %6.2f ms\n", jMs);
        System.out.printf("  Ratio:            %6.2fx %s\n", ratio, ratio < 1.0 ? "FASTER" : "SLOWER");
    }

    private static SimpleDTO createSimpleDTO(String name, int age, String email) {
        SimpleDTO dto = new SimpleDTO();
        dto.setName(name);
        dto.setAge(age);
        dto.setEmail(email);
        dto.setStatus(1);
        dto.setSalary(50000.0 + age * 1000);
        return dto;
    }
}
