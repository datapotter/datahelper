package xyz.jphil.datahelper;

import java.util.List;
import java.util.Map;

/**
 * Base interface for all DataHelper-generated interfaces.
 * Provides core property accessor methods that enable type-safe field access.
 *
 * <p>Generated interfaces implement these abstract property accessor methods.
 * Serialization/deserialization is provided by composable trait interfaces
 * (e.g., DataHelper_Json_Trait, DataHelper_ArcadeDB_Trait) that build upon
 * these property accessors.</p>
 *
 * <p>This interface contains ONLY the minimal property accessor contract.
 * No serialization logic is included here - traits handle that.</p>
 */
public interface DataHelper_I<E extends DataHelper_I<E>> {

    // ========== Abstract Methods (Implemented by Generated Code) ==========

    /**
     * Get the concrete class for this DataHelper instance.
     * Compensates for type erasure in generics.
     *
     * <p>This method provides access to the actual runtime class, which is useful for:
     * - Reflection: Inspecting fields, annotations, methods
     * - Type safety: Using Class&lt;T&gt; instead of raw strings
     * - Schema generation: Building database schemas from class metadata
     * - Factory patterns: Instantiating new instances via reflection
     * </p>
     *
     * @return the concrete class object (e.g., TestPersonDTO.class)
     */
    Class<?> dataClass();

    /**
     * Get all field names for this DTO.
     * Generated code returns an immutable List for safety.
     *
     * @return list of field names
     */
    List<String> fieldNames();

    /**
     * Get property value by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @return the property value, or null if not found
     */
    Object getPropertyByName(String propertyName);

    /**
     * Set property value by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @param value the value to set
     */
    void setPropertyByName(String propertyName, Object value);

    /**
     * Get property type by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @return the property type, or null if not found
     */
    Class<?> getPropertyType(String propertyName);

    /**
     * Create nested object for a property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have nested DataHelper objects.</p>
     *
     * @param propertyName the property name
     * @return new instance of nested object, or null if property is not a nested DataHelper object
     * @throws UnsupportedOperationException if not overridden and called
     */
    default DataHelper_I<?> createNestedObject(String propertyName) {
        throw new UnsupportedOperationException(
            "createNestedObject() not implemented for property: " + propertyName);
    }

    /**
     * Create list element for a list property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have List fields with DataHelper elements.</p>
     *
     * @param propertyName the property name (must be a list field)
     * @return new instance of list element, or null if property is not a list of DataHelper objects
     * @throws UnsupportedOperationException if not overridden and called
     */
    default DataHelper_I<?> createListElement(String propertyName) {
        throw new UnsupportedOperationException(
            "createListElement() not implemented for property: " + propertyName);
    }

    /**
     * Check if property is a list field.
     * Generated code uses simple equality check or switch.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.
     * Hand-written DataHelper_I implementations must explicitly return true/false.</p>
     *
     * @param propertyName the property name
     * @return true if property is a List
     */
    boolean isListField(String propertyName);

    /**
     * Check if property is a nested DataHelper object field.
     * Generated code uses simple equality check or switch.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.
     * Hand-written DataHelper_I implementations must explicitly return true/false.</p>
     *
     * @param propertyName the property name
     * @return true if property is a DataHelper_I object
     */
    boolean isNestedObjectField(String propertyName);

    /**
     * Check if property is a map field.
     * Generated code uses simple equality check or switch.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.
     * Hand-written DataHelper_I implementations must explicitly return true/false.</p>
     *
     * @param propertyName the property name
     * @return true if property is a Map
     */
    boolean isMapField(String propertyName);

    /**
     * Get map key type for a property.
     * Generated code uses switch statement.
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have Map fields.</p>
     *
     * @param propertyName the property name
     * @return the map key type, or null if not a map field
     * @throws UnsupportedOperationException if not overridden and called
     */
    default Class<?> getMapKeyType(String propertyName) {
        throw new UnsupportedOperationException(
            "getMapKeyType() not implemented for property: " + propertyName);
    }

    /**
     * Get map value type for a property.
     * Generated code uses switch statement.
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have Map fields.</p>
     *
     * @param propertyName the property name
     * @return the map value type, or null if not a map field
     * @throws UnsupportedOperationException if not overridden and called
     */
    default Class<?> getMapValueType(String propertyName) {
        throw new UnsupportedOperationException(
            "getMapValueType() not implemented for property: " + propertyName);
    }

    /**
     * Create map instance for a property.
     * Generated code uses switch statement with direct instantiation.
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have Map fields.</p>
     *
     * @param propertyName the property name
     * @return new Map instance, or null if not a map field
     * @throws UnsupportedOperationException if not overridden and called
     */
    default Map<?, ?> createMapInstance(String propertyName) {
        throw new UnsupportedOperationException(
            "createMapInstance() not implemented for property: " + propertyName);
    }

    /**
     * Check if map value is a DataHelper type.
     * Generated code uses simple equality check or switch.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.
     * Hand-written DataHelper_I implementations must explicitly return true/false.</p>
     *
     * @param propertyName the property name
     * @return true if map value type is DataHelper_I
     */
    boolean isMapValueDataHelper(String propertyName);

    /**
     * Create map value element for a map property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written DataHelper_I implementations must override this if they have Map fields with DataHelper values.</p>
     *
     * @param propertyName the property name (must be a map field with DataHelper values)
     * @return new instance of map value element, or null if property is not a map of DataHelper objects
     * @throws UnsupportedOperationException if not overridden and called
     */
    default DataHelper_I<?> createMapValueElement(String propertyName) {
        throw new UnsupportedOperationException(
            "createMapValueElement() not implemented for property: " + propertyName);
    }

    // ========== Helper Methods ==========

    /**
     * Convert value to target type.
     * Handles common numeric conversions (Number to Integer/Long/Double/Float).
     *
     * <p>This helper method is used by serialization traits to convert values
     * when reading from external formats (JSON, databases, etc.).</p>
     *
     * @param value the value to convert
     * @param targetType the target type
     * @return converted value, or original if no conversion needed
     */
    static Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == null) return value;
        if (targetType.isInstance(value)) return value;

        // String to numeric conversions (for Map keys, JSON parsing, etc.)
        if (value instanceof String) {
            String str = (String) value;
            try {
                if (targetType == Integer.class || targetType == int.class) {
                    return Integer.parseInt(str);
                } else if (targetType == Long.class || targetType == long.class) {
                    return Long.parseLong(str);
                } else if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(str);
                } else if (targetType == Float.class || targetType == float.class) {
                    return Float.parseFloat(str);
                } else if (targetType == Short.class || targetType == short.class) {
                    return Short.parseShort(str);
                } else if (targetType == Byte.class || targetType == byte.class) {
                    return Byte.parseByte(str);
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return Boolean.parseBoolean(str);
                }
            } catch (NumberFormatException e) {
                // Fall through to return original value
            }
        }

        // Numeric conversions
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return num.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return num.byteValue();
            }
        }

        return value;
    }
}
