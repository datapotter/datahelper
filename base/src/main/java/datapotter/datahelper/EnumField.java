package datapotter.datahelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Type-safe field descriptor for an enum-valued field, carrying the field's own storage resolver.
 *
 * <p>An enum field used to get a plain {@link Field}, a name and a class and nothing else, while the
 * knowledge of how to turn a stored string back into a constant lived somewhere else entirely — a
 * separate generated static map per field per entity, plus a branch in
 * {@code resolveEnumFromStorage} choosing between {@code uuid} and {@code name}. The descriptor and
 * the behaviour belonged together, so they are together: every enum field, however it stores itself,
 * answers {@code $field.fromStorage(value)}.
 *
 * <p><b>Three ways a resolver is built, one call shape at the use site.</b>
 * <ul>
 *   <li>{@link #generated} — the enum carries {@code @EnumData}, so it owns a generated
 *       {@code Foo_I.fromStorage}: a compile-time {@code switch}, allocating nothing. This is the
 *       one to prefer; the lookup lives once with the enum instead of once per consuming field.</li>
 *   <li>{@link #byUuid} / {@link #byName} — a plain {@code @AsUuid}/{@code @AsName} enum, indexed
 *       here from constants the caller supplies.</li>
 * </ul>
 *
 * <p><b>Why the constants are passed in rather than read from the {@code Class}.</b>
 * {@code Class.getEnumConstants()} is reflection, which this codebase avoids so generated code stays
 * usable under TeaVM. A generic class cannot call {@code T.values()}, so generated code hands its own
 * {@code Foo.values()} in — a synthetic method, not reflection.
 *
 * @param <E> the DataHelper entity type this field belongs to
 * @param <T> the enum type of this field's value
 * @see EnumListField
 * @see EnumData
 */
public final class EnumField<E extends DataHelper_I<E>, T extends Enum<T>>
        implements Field_I<E, T> {

    private final String name;
    private final Class<T> type;
    private final Function<String, T> resolver;

    /** The field name, for the terse {@code $field.__} form the other descriptors also carry. */
    public final String __;

    private EnumField(String name, Class<T> type, Function<String, T> resolver) {
        this.name = name;
        this.type = type;
        this.resolver = resolver;
        this.__ = name;
    }

    @Override public String name() { return name; }
    @Override public Class<T> type() { return type; }

    /**
     * The constant a stored string resolves to, or {@code null}.
     *
     * <p>Never throws. A value matching no constant was written by newer code than this build, which
     * is not the same as corrupt — so the caller gets {@code null} and decides.
     */
    public T fromStorage(String storedValue) {
        return storedValue == null ? null : resolver.apply(storedValue);
    }

    /** For an {@code @EnumData} enum: resolve through the enum's own generated {@code fromStorage}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T>>
    EnumField<E, T> generated(String name, Class<T> type, Function<String, T> fromStorage) {
        return new EnumField<>(name, type, fromStorage);
    }

    /** For a plain {@code @AsUuid} enum: index the given constants by {@link HasUuid#uuid()}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T> & HasUuid>
    EnumField<E, T> byUuid(String name, Class<T> type, T[] constants) {
        return new EnumField<>(name, type, index(constants, HasUuid::uuid));
    }

    /** For a plain {@code @AsName} enum: index the given constants by {@link Enum#name()}. */
    public static <E extends DataHelper_I<E>, T extends Enum<T>>
    EnumField<E, T> byName(String name, Class<T> type, T[] constants) {
        return new EnumField<>(name, type, index(constants, Enum::name));
    }

    /** Shared with {@link EnumListField}: a miss returns null, which is the never-throws rule. */
    static <T> Function<String, T> index(T[] constants, Function<T, String> key) {
        Map<String, T> byKey = new HashMap<>(Math.max(4, constants.length * 2));
        for (T c : constants) byKey.put(key.apply(c), c);
        return byKey::get;
    }

    @Override
    public String toString() {
        return "EnumField[" + name + ": " + type.getSimpleName() + "]";
    }
}
