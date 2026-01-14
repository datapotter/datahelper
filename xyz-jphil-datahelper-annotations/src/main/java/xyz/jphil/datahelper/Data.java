package xyz.jphil.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for DataHelper code generation using the sealed abstract class pattern.
 *
 * <p>Generates a sealed abstract parent class (with _A suffix) containing:
 * <ul>
 *   <li>Field constants ($name, $email, etc.)</li>
 *   <li>FIELDS list</li>
 *   <li>Delegating getters/setters</li>
 *   <li>Fluent accessors</li>
 *   <li>DataHelper_I implementation (15 property accessor methods)</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * {@code @Data}
 * public final class Person extends Person_A {
 *     String name;
 *     String email;
 *     Integer age;
 * }
 * </pre>
 *
 * <h3>Design Notes:</h3>
 * <ul>
 *   <li><b>Sealed Class:</b> Parent class is sealed and permits only the annotated class</li>
 *   <li><b>Package-Private Fields:</b> Child class fields must be package-private (no modifier) for parent delegation</li>
 *   <li><b>Final Child:</b> Child class should be {@code final} to complete the sealed hierarchy</li>
 *   <li><b>No Lombok Needed:</b> Parent provides all getters/setters via delegation</li>
 *   <li><b>Clean Design:</b> Similar to @ArcadeData but at the base DataHelper level</li>
 * </ul>
 *
 * <h3>Comparison with @DataHelper:</h3>
 * <ul>
 *   <li><b>@DataHelper:</b> Generates interface (_I suffix), requires Lombok @Data on child class</li>
 *   <li><b>@Data:</b> Generates sealed abstract class (_A suffix), no Lombok needed, less verbose</li>
 * </ul>
 *
 * @see DataHelper_I
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Data {
}
