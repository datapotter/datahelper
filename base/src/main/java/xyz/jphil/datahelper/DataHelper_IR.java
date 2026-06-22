package xyz.jphil.datahelper;

import java.util.List;

/**
 * Readable contract for all DataHelper-generated types.
 *
 * <p>This is the <em>read side</em> of {@link DataHelper_I}: it promises that an instance
 * can be inspected (field names, property values, property/metadata types) but makes no
 * promise about mutability. A mutable DTO, its sealed {@code _A} base, and an immutable
 * {@code _R} record projection are all {@code DataHelper_IR} — so read-only consumers
 * (serialization, value-based {@code equals}/{@code hashCode}/{@code toString}, schema
 * inspection) can be grounded here and work uniformly across mutable instances and records.</p>
 *
 * <p>The name is deliberately <em>readable</em>, not <em>read-only</em> / <em>immutable</em>:
 * a mutable {@code DataHelper_I} is also a {@code DataHelper_IR}, so this type only guarantees
 * "readable through this reference" (cf. .NET {@code IReadOnly*} vs {@code IImmutable*}).
 * Immutability is the {@code _R} record's guarantee.</p>
 *
 * <p>The write side — {@code setPropertyByName} and the {@code create*} factories used during
 * deserialization — lives on {@link DataHelper_I}, which extends this interface.</p>
 *
 * @param <E> the self type (for fluent/self-referential generics)
 */
public interface DataHelper_IR<E extends DataHelper_IR<E>> {

    // ========== Abstract Methods (Implemented by Generated Code) ==========

    /**
     * Get the concrete class for this DataHelper instance.
     * Compensates for type erasure in generics.
     *
     * <p>Useful for reflection, type-safe {@code Class<T>} handling, schema generation,
     * and factory patterns.</p>
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
     * Get property type by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @return the property type, or null if not found
     */
    Class<?> getPropertyType(String propertyName);

    /**
     * Check if property is a list field.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.
     * Hand-written implementations must explicitly return true/false.</p>
     *
     * @param propertyName the property name
     * @return true if property is a List
     */
    boolean isListField(String propertyName);

    /**
     * Check if property is a nested DataHelper object field.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.</p>
     *
     * @param propertyName the property name
     * @return true if property is a DataHelper object
     */
    boolean isNestedObjectField(String propertyName);

    /**
     * Check if property is a map field.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.</p>
     *
     * @param propertyName the property name
     * @return true if property is a Map
     */
    boolean isMapField(String propertyName);

    /**
     * Check if map value is a DataHelper type.
     *
     * <p><strong>MUST be implemented</strong> - no default provided.</p>
     *
     * @param propertyName the property name
     * @return true if map value type is a DataHelper type
     */
    boolean isMapValueDataHelper(String propertyName);

    /**
     * Get map key type for a property.
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written implementations must override this if they have Map fields.</p>
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
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Hand-written implementations must override this if they have Map fields.</p>
     *
     * @param propertyName the property name
     * @return the map value type, or null if not a map field
     * @throws UnsupportedOperationException if not overridden and called
     */
    default Class<?> getMapValueType(String propertyName) {
        throw new UnsupportedOperationException(
            "getMapValueType() not implemented for property: " + propertyName);
    }
}
