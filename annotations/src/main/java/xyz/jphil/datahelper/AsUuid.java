package xyz.jphil.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an enum as storing its identity as a 128-bit uuid rather than its constant name.
 *
 * <p>Goes on the ENUM, not the field — fields of the enum type need no annotation at all:
 * <pre>
 * {@code @AsUuid(UuidEncoding.BASE64URL)}
 * public enum Outcome implements HasUuid {
 *     UNKNOWN("6BS3GY9NvpyzKA1MV-AHkA"),
 *     FAVOURABLE("pZzI2NQXc1X4eyVUzR9p9w");
 *     ...
 * }
 * </pre>
 *
 * <p><b>Mandatory and mutually exclusive with {@link AsName}, with no default.</b> Storage form is
 * declared, never inferred: adding {@code implements HasUuid} to an enum must never silently change
 * what its constants store. The encoding is mandatory for the same reason JPA's
 * {@code @Enumerated(EnumType.ORDINAL)} is a well-known trap — {@code ORDINAL} being the
 * <em>default</em> is what lets reordering constants silently corrupt stored data. This annotation
 * has nothing to omit.
 *
 * <p>The base annotation processor validates every enum carrying this annotation at its own
 * declaration, whether or not anything references it yet: {@link #value()} must be present, the
 * enum must {@code implement HasUuid}, it must not also carry {@link AsName}, and every constant's
 * uuid literal must match the declared encoding's alphabet and fixed length.
 *
 * @see AsName
 * @see HasUuid
 * @see UuidEncoding
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AsUuid {
    /** The encoding the uuid literals are written in. No default — a genuine choice, always stated. */
    UuidEncoding value();
}
