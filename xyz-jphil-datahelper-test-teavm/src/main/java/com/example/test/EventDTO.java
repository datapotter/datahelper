package com.example.test;

import lombok.Data;
import org.teavm.jso.JSProperty;
import xyz.jphil.datahelper.DataHelper;

/**
 * Test DTO that uses a hand-written DataHelper_I implementation.
 * Tests that the processor recognizes CustomDateWrapper as a nested DataHelper
 * even though it doesn't have @DataHelper annotation.
 */
@DataHelper(
    propertyAnnotations = {JSProperty.class}
)
@Data
public class EventDTO implements EventDTO_I<EventDTO> {
    String eventName;
    CustomDateWrapper eventDate; // Hand-written DataHelper_I (no annotation)
    String location;
}
