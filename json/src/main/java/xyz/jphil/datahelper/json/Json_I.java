package xyz.jphil.datahelper.json;

import xyz.jphil.datahelper.DataHelper_I;

import java.util.Map;

/**
 * Write side of the JSON trait: deserialization ({@code fromJson}).
 *
 * <p>Extends {@link Json_IR} (which carries {@code toJson}) and {@link DataHelper_I} (the
 * write contract). A DTO implementing {@code Json_I} therefore has both {@code toJson} and
 * {@code fromJson} — existing DTOs are unaffected by the split. The read-only {@code toJson}
 * half ({@link Json_IR}) can be mixed into readable {@code _IR} interfaces so immutable
 * {@code _R} record projections can also serialize.</p>
 *
 * <p>Records are not deserialized into directly: parse into the mutable form with
 * {@code fromJson}, then call {@code toRecord()}.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * @DataHelper
 * public class PersonDTO {
 *     String name;
 *     Integer age;
 * }
 *
 * // Use JSON methods directly
 * PersonDTO person = new PersonDTO();
 * String json = person.toJson();
 * person.fromJson(json);
 * }</pre>
 *
 * <p><strong>Performance Note:</strong></p>
 * <ul>
 *   <li>JVM: Good performance, suitable for production use</li>
 *   <li>TeaVM: Performance penalty - avoid using JSON on TeaVM, use JSObject directly</li>
 * </ul>
 *
 * @param <E> the implementing type (self-reference for fluent API)
 */
public interface Json_I<E extends DataHelper_I<E>> extends Json_IR<E>, DataHelper_I<E> {

    /**
     * Populate this DTO from a JSON string.
     *
     * <p>The parsed object is a plain name&rarr;value map, and reading one of those into a
     * DataHelper is not a JSON concern — it is {@link DataHelper_I#fromMap(Map)}, in {@code base}.
     * So this method is the parse and nothing more. Doing it this way is also what makes a nested
     * {@code @Data} block readable: this trait's own walk could only recurse into a nested object
     * that implemented {@code Json_I}, which an embedded block does not, so such a block came back
     * {@code null} and a list of them came back holding raw maps.</p>
     *
     * @param json the JSON string (must be a JSON object)
     * @return this instance for chaining
     * @throws xyz.jphil.datahelper.json.MinimalJsonParser.JsonParseException if JSON is malformed
     */
    default E fromJson(String json) {
        return fromMap(MinimalJsonParser.parseObject(json));
    }
}
