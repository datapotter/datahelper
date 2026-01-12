package com.example.test;

import xyz.jphil.datahelper.DataHelper_I;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Hand-written DataHelper_I implementation (no @DataHelper annotation).
 * This tests Case 2: implements DataHelper_I but no annotation.
 *
 * Wraps LocalDate with custom string conversion.
 */
public class CustomDateWrapper implements DataHelper_I<CustomDateWrapper> {
    private LocalDate date;

    public CustomDateWrapper() {
    }

    public CustomDateWrapper(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public Class<?> dataClass() {
        return CustomDateWrapper.class;
    }

    @Override
    public List<String> fieldNames() {
        return List.of("date");
    }

    @Override
    public Object getPropertyByName(String propertyName) {
        if ("date".equals(propertyName)) {
            // Convert LocalDate to String for serialization
            return date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
        }
        return null;
    }

    @Override
    public void setPropertyByName(String propertyName, Object value) {
        if ("date".equals(propertyName)) {
            // Convert String to LocalDate for deserialization
            if (value instanceof String) {
                this.date = LocalDate.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (value == null) {
                this.date = null;
            }
        }
    }

    @Override
    public Class<?> getPropertyType(String propertyName) {
        if ("date".equals(propertyName)) {
            return String.class; // We serialize as String
        }
        return null;
    }

    @Override
    public DataHelper_I<?> createNestedObject(String propertyName) {
        return null; // No nested objects
    }

    @Override
    public DataHelper_I<?> createListElement(String propertyName) {
        return null; // No lists
    }

    @Override
    public boolean isListField(String propertyName) {
        return false;
    }

    @Override
    public boolean isNestedObjectField(String propertyName) {
        return false; // No nested objects
    }

    @Override
    public boolean isMapField(String propertyName) {
        return false; // No maps
    }

    @Override
    public boolean isMapValueDataHelper(String propertyName) {
        return false; // No maps
    }

    // Factory methods use default implementations (throw UnsupportedOperationException)
    // No need to override: createNestedObject, createListElement,
    // getMapKeyType, getMapValueType, createMapInstance, createMapValueElement
}
