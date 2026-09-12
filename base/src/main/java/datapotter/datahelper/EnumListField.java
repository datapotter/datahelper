package datapotter.datahelper;

import java.util.List;
import java.util.function.Function;

/**
 * Type-safe field descriptor for a {@code List<E>} field whose element type is an enum.
 *
 * <p>The same idea as {@link EnumField}, for the list shape, and it stands beside it exactly as
 * {@link ListDataField} stands beside {@link DataField}. {@link #fromStorage} resolves one ELEMENT,
 * because a list arrives as a list of stored strings and is resolved element-wise.
 *
 * @param <E> the DataHelper entity type this field belongs to
 * @param <T> the enum type of the list's elements
 * @see EnumField
 */
public final class EnumListField<E extends DataHelper_I<E>, T extends Enum<T>>
        implements Field_I<E, List<T>> {

    private final String name;
    private final Class<T> elementType;
    private final Function<String, T> resolver;

    /** The field name, for the terse {@code $field.__} form the other descriptors also carry. */
    public final String __;

    private EnumListField(String name, Class<T> elementType, Function<String, T> resolver) {
        this.name = name;
        this.elementType = elementType;
        this.resolver = resolver;
        this.__ = name;
    }

    @Override public String name() { return name; }

    @SuppressWarnings("unchecked")
    @Override public Class<List<T>> type() { return (Class<List<T>>) (Class<?>) List.class; }

    /** The enum type of the elements, which {@link #type()} erases to {@code List}. */
    public Class<T> elementType() { return elementType; }

    /** The constant one stored element resolves to, or {@code null}. Never throws. */
    public T fromStorage(String storedValue) {
        return storedValue == null ? null : resolver.apply(storedValue);
    }

    /** For an {@code @EnumData} element enum: resolve through its own generated {@code fromStorage}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T>>
    EnumListField<E, T> generated(String name, Class<T> elementType, Function<String, T> fromStorage) {
        return new EnumListField<>(name, elementType, fromStorage);
    }

    /** For a plain {@code @AsUuid} element enum: index the given constants by {@link HasUuid#uuid()}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T> & HasUuid>
    EnumListField<E, T> byUuid(String name, Class<T> elementType, T[] constants) {
        return new EnumListField<>(name, elementType, EnumField.index(constants, HasUuid::uuid));
    }

    /** For a plain {@code @AsName} element enum: index the given constants by {@link Enum#name()}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T>>
    EnumListField<E, T> byName(String name, Class<T> elementType, T[] constants) {
        return new EnumListField<>(name, elementType, EnumField.index(constants, Enum::name));
    }

    @Override
    public String toString() {
        return "EnumListField[" + name + ": List<" + elementType.getSimpleName() + ">]";
    }
}
