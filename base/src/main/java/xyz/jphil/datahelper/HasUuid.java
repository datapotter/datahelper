package xyz.jphil.datahelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implemented by an enum annotated {@code @AsUuid}, so its constants carry an identity separate
 * from their name. Lives in {@code base} (not the compile-only {@code annotations} jar) because
 * user enums implement it and need it at runtime.
 *
 * @see #storageValue(Object)
 */
public interface HasUuid {

    /** This constant's 128-bit id, in the encoding declared on the enum's {@code @AsUuid}. */
    String uuid();

    /**
     * The wire form of a value about to cross the storage boundary: an {@link HasUuid} constant
     * becomes its {@link #uuid()}, a plain enum becomes its {@link Enum#name()}, a {@link Collection}
     * is mapped element-wise (for {@code IN}-style queries), and anything else passes through
     * unchanged.
     *
     * <p>Deliberately a plain {@code instanceof} dispatch on the runtime value rather than generated
     * per-field code — the concrete type is known at the call site, so no reflection is needed to
     * decide which conversion applies. This is the write-side counterpart to the generated,
     * per-field {@code resolveEnumFromStorage} used on read (where only a {@code Class<?>} handle is
     * available and the concrete constant set cannot be recovered without reflection).
     */
    static Object storageValue(Object value) {
        if (value instanceof HasUuid hu) return hu.uuid();
        if (value instanceof Enum<?> en) return en.name();
        if (value instanceof Collection<?> coll) {
            List<Object> out = new ArrayList<>(coll.size());
            for (Object o : coll) out.add(storageValue(o));
            return out;
        }
        return value;
    }
}
