package com.example.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSMapLike;
import xyz.jphil.datahelper.teavm.TeaVMMapLike;

import java.time.LocalDate;

/**
 * Test for hand-written DataHelper_I implementations (without @DataHelper annotation).
 *
 * This validates that the annotation processor correctly recognizes custom implementations
 * of DataHelper_I and generates proper nested object handling for them.
 */
public class CustomDataHelperTest {

    public static void main(String[] args) {
        log("=== Custom DataHelper_I Implementation Test ===\n");

        testCustomDateWrapper();
        testEventDTOWithCustomDateWrapper();

        log("\n=== All custom DataHelper tests completed! ===");
    }

    /**
     * Test 1: CustomDateWrapper alone (hand-written DataHelper_I)
     */
    private static void testCustomDateWrapper() {
        log("--- Test 1: Hand-written DataHelper_I (CustomDateWrapper) ---");

        // Create directly
        CustomDateWrapper wrapper = new CustomDateWrapper(LocalDate.of(2024, 1, 15));
        log("Original date: " + wrapper.getDate());

        // Test serialization
        String dateStr = (String) wrapper.getPropertyByName("date");
        log("Serialized to: " + dateStr);

        // Test deserialization
        CustomDateWrapper wrapper2 = new CustomDateWrapper();
        wrapper2.setPropertyByName("date", "2024-06-30");
        log("Deserialized from string: " + wrapper2.getDate());

        log("");
    }

    /**
     * Test 2: EventDTO containing CustomDateWrapper as nested object
     * This validates the processor recognizes CustomDateWrapper as nested DataHelper
     */
    private static void testEventDTOWithCustomDateWrapper() {
        log("--- Test 2: EventDTO with nested CustomDateWrapper ---");

        // Parse JSON with nested date
        String json = "{"
            + "\"eventName\":\"Conference 2024\","
            + "\"eventDate\":{\"date\":\"2024-09-15\"},"
            + "\"location\":\"San Francisco\""
            + "}";

        JSMapLike<JSObject> eventJson = (JSMapLike<JSObject>) parseJSON(json);
        EventDTO event = new EventDTO();
        event.fromMapLike(TeaVMMapLike.wrap(eventJson));

        log("Event Name: " + event.getEventName());
        log("Location: " + event.getLocation());

        CustomDateWrapper dateWrapper = event.getEventDate();
        if (dateWrapper != null) {
            LocalDate date = dateWrapper.getDate();
            log("Event Date: " + date);

            if (date != null && date.equals(LocalDate.of(2024, 9, 15))) {
                log("✓ Date parsed correctly!");
            } else {
                log("✗ ERROR: Date mismatch!");
            }
        } else {
            log("✗ ERROR: eventDate is null!");
        }

        // Test round-trip
        log("\nTesting round-trip...");
        EventDTO roundTrip = new EventDTO();
        roundTrip.fromMapLike(event.toMapLike(false));

        log("After round-trip:");
        log("  Event Name: " + roundTrip.getEventName());
        log("  Location: " + roundTrip.getLocation());

        if (roundTrip.getEventDate() != null) {
            log("  Event Date: " + roundTrip.getEventDate().getDate());
            log("✓ Round-trip successful!");
        } else {
            log("✗ ERROR: Date lost in round-trip!");
        }
    }

    @JSBody(params = {"jsonString"}, script = "return JSON.parse(jsonString);")
    private static native JSObject parseJSON(String jsonString);

    @JSBody(params = {"message"}, script = "console.log(message);")
    private static native void log(String message);
}
