package xyz.jphil.datahelper.json;

import xyz.jphil.datahelper.DataHelper_IR;

/**
 * Read side of the JSON trait: serialization ({@code toJson}).
 *
 * <p>Extends {@link DataHelper_IR}, so it can be mixed into the readable {@code _IR}
 * interface and is therefore available on immutable {@code _R} record projections as well
 * as mutable DTOs. The write side — {@code fromJson} — lives on {@link Json_I}, which
 * deserializes into a mutable instance (records are populated by parsing into the mutable
 * form, then {@code toRecord()}).</p>
 *
 * @param <E> the implementing type (self-reference for fluent API)
 */
public interface Json_IR<E extends DataHelper_IR<E>> extends DataHelper_IR<E> {

    /**
     * Convert this DTO to a JSON string.
     *
     * @param deep if true, recursively serialize nested objects, lists, and maps;
     *             if false, nested objects are included as shallow references
     * @return JSON string representation
     */
    default String toJson(boolean deep) {
        return MinimalJsonWriter.write(this, deep);
    }

    /**
     * Convert this DTO to a JSON string (deep serialization).
     * Convenience method that calls toJson(true).
     *
     * @return JSON string representation (deep serialization)
     */
    default String toJson() {
        return toJson(true);
    }
}
