package xyz.jphil.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an enum as storing its identity as its constant name ({@code Enum.name()}).
 *
 * <p>Goes on the ENUM, not the field:
 * <pre>
 * {@code @AsName}
 * public enum Severity { LOW, MEDIUM, HIGH }
 * </pre>
 *
 * <p>Mutually exclusive with {@link AsUuid} — an enum carrying both is a compile error at its own
 * declaration, and so is an enum carrying neither when a field of that type needs storing (a
 * backend-specific rule; see the {@code @ArcadeData} processor). No parameter: {@code name()} is
 * {@code name()}, there is nothing to configure.
 *
 * @see AsUuid
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AsName {
}
