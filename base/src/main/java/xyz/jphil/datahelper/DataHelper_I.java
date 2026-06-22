package xyz.jphil.datahelper;

import java.util.Map;
import java.util.Objects;

/**
 * Full read+write contract for all DataHelper-generated interfaces.
 *
 * <p>Extends {@link DataHelper_IR} (the readable contract) with the write side:
 * {@code setPropertyByName} and the {@code create*} factory methods used while
 * deserializing into a mutable instance. Mutable DTOs and their sealed {@code _A}
 * bases implement this; immutable {@code _R} record projections implement only
 * {@link DataHelper_IR}.</p>
 *
 * <p>This interface contains ONLY the property accessor contract. Serialization is
 * provided by composable trait interfaces (e.g., Json, ArcadeDB) that build upon
 * these accessors.</p>
 */
public interface DataHelper_I<E extends DataHelper_I<E>> extends DataHelper_IR<E> {

    // ========== Abstract / Default Write Methods (Implemented by Generated Code) ==========

    /**
     * Set property value by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @param value the value to set
     */
    void setPropertyByName(String propertyName, Object value);

    /**
     * Create nested object for a property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written implementations must override this if they have nested DataHelper objects.</p>
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
     * Hand-written implementations must override this if they have List fields with DataHelper elements.</p>
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
     * Create map instance for a property.
     * Generated code uses switch statement with direct instantiation.
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written implementations must override this if they have Map fields.</p>
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
     * Create map value element for a map property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written implementations must override this if they have Map fields with DataHelper values.</p>
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
     * <p>Kept on {@code DataHelper_I} (not relocated to {@link DataHelper_IR}) so all
     * generated {@code DataHelper_I.convertType(...)} call sites keep resolving — static
     * interface methods are not inherited.</p>
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

    // ========== Generic Object-method Helpers ==========
    // These build on the readable accessors (DataHelper_IR) so generated code can delegate
    // equals/hashCode/toString to a single implementation (no per-field codegen). Grounded on
    // DataHelper_IR so they also apply to immutable record projections, not just mutable DTOs.

    /**
     * Generic value-based equality over all fields.
     *
     * <p>Two instances are equal iff they share the same {@link DataHelper_IR#dataClass()} and
     * every field value (by {@link DataHelper_IR#fieldNames()}) is equal via {@link Objects#equals}.
     * Nested DataHelper/List/Map values compare by their own {@code equals}.</p>
     *
     * <p>Intended for {@code Object.equals(Object)} delegation in generated mutable code.
     * Caveat: because equality is over mutable fields, a mutable DTO's equality (and
     * {@link #hashCode(DataHelper_IR)}) changes if a field is mutated while held in a
     * hash-based collection. (The {@code _R} record projection has stable value semantics.)</p>
     *
     * @param self  this DataHelper instance (the caller)
     * @param other the object to compare against
     * @return true if {@code other} is a DataHelper of the same data class with equal field values
     */
    static boolean equals(DataHelper_IR<?> self, Object other) {
        if (self == other) return true;
        if (self == null || other == null) return false;
        if (!(other instanceof DataHelper_IR<?> that)) return false;
        if (self.dataClass() != that.dataClass()) return false;
        for (String fieldName : self.fieldNames()) {
            if (!Objects.equals(self.getPropertyByName(fieldName), that.getPropertyByName(fieldName))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Generic field-based hash code, consistent with {@link #equals(DataHelper_IR, Object)}.
     *
     * @param self this DataHelper instance
     * @return a hash code derived from all field values
     */
    static int hashCode(DataHelper_IR<?> self) {
        if (self == null) return 0;
        int result = 1;
        for (String fieldName : self.fieldNames()) {
            Object value = self.getPropertyByName(fieldName);
            result = 31 * result + (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    /**
     * Generic string representation: {@code SimpleClassName{field1=value1, field2=value2}}.
     *
     * @param self this DataHelper instance
     * @return a readable representation listing all fields
     */
    static String toString(DataHelper_IR<?> self) {
        if (self == null) return "null";
        StringBuilder sb = new StringBuilder(self.dataClass().getSimpleName()).append('{');
        boolean first = true;
        for (String fieldName : self.fieldNames()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(fieldName).append('=').append(self.getPropertyByName(fieldName));
        }
        return sb.append('}').toString();
    }
}
