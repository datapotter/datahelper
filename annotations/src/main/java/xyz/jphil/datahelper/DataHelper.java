package xyz.jphil.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates {ClassName}_I interface with field symbols and fluent methods.
 *
 * <p>Usage: Annotate DTO class with @Data (Lombok verbose getters/setters required)</p>
 *
 * <p>Generates:</p>
 * <ul>
 *   <li>Interface named {ClassName}_I</li>
 *   <li>Field symbols as $fieldName constants</li>
 *   <li>FIELDS list constant</li>
 *   <li>Verbose getter/setter declarations (for Lombok @Data)</li>
 *   <li>Fluent getter/setter default methods (delegate to verbose)</li>
 *   <li>toMap() and fromMap() utility methods</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 * {@code
 * @GenDTOSymbolsInterface
 * @Data
 * public class MyDTO implements MyDTO_I<MyDTO> {
 *     String name;
 *     Integer age;
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DataHelper {
    /**
     * Optional annotation classes to be applied to generated verbose getter and setter methods.
     * This is useful for frameworks like TeaVM that require specific annotations like @JSProperty
     * on getters and setters.
     *
     * <p>Example:</p>
     * <pre>
     * {@code
     * @GenDTOSymbolsInterface(propertyAnnotations = {JSProperty.class}, superInterfaces = {JSObject.class})
     * @Data
     * public class MyDTO implements MyDTO_I<MyDTO> {
     *     String name;
     * }
     * }
     * </pre>
     */
    Class<?>[] propertyAnnotations() default {};

    /**
     * Optional super interfaces that the generated interface should extend.
     * This is useful for frameworks like TeaVM where interfaces with @JSProperty methods
     * must extend JSObject, or for any other framework-specific interface requirements.
     *
     * <p>Example:</p>
     * <pre>
     * {@code
     * @GenDTOSymbolsInterface(propertyAnnotations = {JSProperty.class}, superInterfaces = {JSObject.class})
     * @Data
     * public class MyDTO implements MyDTO_I<MyDTO> {
     *     String name;
     * }
     * }
     * </pre>
     */
    Class<?>[] superInterfaces() default {};
}
