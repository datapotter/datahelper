package com.example.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Analyze JIT warmup effects by measuring performance at intervals.
 * Shows how performance changes as JIT optimizes the code.
 */
public class JitWarmupAnalysis {

    private static final int BATCH_SIZE = 500;
    private static final int TOTAL_ITERATIONS = 5000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.out.println("=== JIT Warmup Analysis ===");
        System.out.println("Measuring performance every " + BATCH_SIZE + " iterations");
        System.out.println("Total iterations: " + TOTAL_ITERATIONS + "\n");

        SimpleDTO sample = createSimpleDTO("Test User", 30, "test@example.com");

        System.out.println("───────────────────────────────────────────────────────────────────────");
        System.out.println("SERIALIZATION (Object → JSON String)");
        System.out.println("───────────────────────────────────────────────────────────────────────");
        System.out.printf("%-10s | %-15s | %-15s | %-10s%n", "Iteration", "DataHelper (ms)", "Jackson (ms)", "Ratio");
        System.out.println("───────────────────────────────────────────────────────────────────────");

        int batches = TOTAL_ITERATIONS / BATCH_SIZE;

        for (int batch = 1; batch <= batches; batch++) {
            int iterationNum = batch * BATCH_SIZE;

            // Test DataHelper Direct
            long start = System.nanoTime();
            for (int i = 0; i < BATCH_SIZE; i++) {
                DirectJsonWriter.toJson(sample);
            }
            long dhTime = System.nanoTime() - start;

            // Test Jackson
            start = System.nanoTime();
            for (int i = 0; i < BATCH_SIZE; i++) {
                objectMapper.writeValueAsString(sample);
            }
            long jTime = System.nanoTime() - start;

            double dhMs = dhTime / 1_000_000.0;
            double jMs = jTime / 1_000_000.0;
            double ratio = dhMs / jMs;

            System.out.printf("%-10d | %15.2f | %15.2f | %10.2fx%n", iterationNum, dhMs, jMs, ratio);
        }

        System.out.println("\n───────────────────────────────────────────────────────────────────────");
        System.out.println("DESERIALIZATION (JSON String → Object)");
        System.out.println("───────────────────────────────────────────────────────────────────────");
        System.out.printf("%-10s | %-15s | %-15s | %-10s%n", "Iteration", "DataHelper (ms)", "Jackson (ms)", "Ratio");
        System.out.println("───────────────────────────────────────────────────────────────────────");

        String dhJson = DirectJsonWriter.toJson(sample);
        String jJson = objectMapper.writeValueAsString(sample);

        for (int batch = 1; batch <= batches; batch++) {
            int iterationNum = batch * BATCH_SIZE;

            // Test DataHelper Direct (JSON → Map → Object)
            long start = System.nanoTime();
            for (int i = 0; i < BATCH_SIZE; i++) {
                Map<String, Object> m = SimpleJsonReader.fromJson(dhJson);
                new SimpleDTO().fromMap(m);
            }
            long dhTime = System.nanoTime() - start;

            // Test Jackson (JSON → Object)
            start = System.nanoTime();
            for (int i = 0; i < BATCH_SIZE; i++) {
                objectMapper.readValue(jJson, SimpleDTO.class);
            }
            long jTime = System.nanoTime() - start;

            double dhMs = dhTime / 1_000_000.0;
            double jMs = jTime / 1_000_000.0;
            double ratio = dhMs / jMs;

            System.out.printf("%-10d | %15.2f | %15.2f | %10.2fx%n", iterationNum, dhMs, jMs, ratio);
        }

        System.out.println("───────────────────────────────────────────────────────────────────────");
        System.out.println("\n=== Analysis Complete ===");
        System.out.println("\nKey observations:");
        System.out.println("- Ratio < 1.0 means DataHelper is FASTER");
        System.out.println("- Ratio > 1.0 means DataHelper is SLOWER");
        System.out.println("- Watch how ratios change as JIT warms up!");
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
