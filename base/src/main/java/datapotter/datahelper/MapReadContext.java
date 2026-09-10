package datapotter.datahelper;

import java.util.Map;

/**
 * How a backend presents its stored values to {@link MapReads}.
 *
 * <p>{@link MapReads} walks a name&rarr;value map and populates a {@link DataHelper_I} using only
 * the generated accessor contract, so it knows nothing about any particular store. The two things
 * it cannot answer on its own are supplied here:</p>
 *
 * <ul>
 *   <li>{@link #asFieldMap(Object)} &mdash; is this stored value a <em>record</em> (something with
 *       named fields, to be read into a nested DataHelper), and if so what are its fields? A plain
 *       {@link Map} qualifies everywhere; a backend adds its own record types (an ArcadeDB
 *       {@code Document}, a query {@code Result}).</li>
 *   <li>{@link #readTraitField} &mdash; a field shape that only that backend has (an ArcadeDB
 *       reference, stored as a RID rather than as an embedded copy).</li>
 * </ul>
 *
 * <p>The context travels down the recursion with the walk, not with the object being populated.
 * That is deliberate and it is the whole reason this type exists: an embedded block is a plain
 * {@code @Data} type with no knowledge of any database, yet the values inside it arrive in the
 * outer backend's native form, so the outer backend's context must still be in force two levels
 * down.</p>
 */
public interface MapReadContext {

    /**
     * Plain maps and nothing else: no backend record types, no backend-specific field shapes.
     * The context for reading a DataHelper out of an ordinary {@link Map} &mdash; a parsed JSON
     * object, a hand-built fixture, the output of {@link MapWrites#toMap(DataHelper_IR)}.
     */
    @SuppressWarnings("unchecked")
    MapReadContext PLAIN = value ->
            value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;

    /**
     * The fields of {@code value} if it is a record in this backend's terms, else {@code null}.
     *
     * <p>Returning {@code null} is the answer "this is a scalar", and it is what tells
     * {@link MapReads} to treat the value as a simple field rather than as something to read into
     * a nested DataHelper. The returned map is read, never retained or mutated.</p>
     */
    Map<String, Object> asFieldMap(Object value);

    /**
     * Read a field whose shape belongs to this backend alone, returning {@code true} if it did.
     *
     * <p>Consulted before every other rule, so a backend can claim a field outright. The default
     * claims nothing. {@code target} is the instance being populated (it carries the generated
     * metadata for {@code fieldName} and the setter), not necessarily the object the walk started
     * on &mdash; nested blocks are populated through this same hook.</p>
     */
    default boolean readTraitField(DataHelper_I<?> target, String fieldName, Object value) {
        return false;
    }
}
