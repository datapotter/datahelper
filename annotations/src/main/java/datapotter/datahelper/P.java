package datapotter.datahelper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field's stable identity (PRP-28 phase 2): six base64url characters, five generated at random
 * plus one check character, minted once and never hand-authored.
 *
 * <pre>
 * {@code @P("o3KcQR")} String outcome;
 * </pre>
 *
 * <p><b>Minted, never authored.</b> A field with no {@code @P} under
 * {@code @ArcadeData(requireIds = true)} fails the build with a freshly generated id in the message,
 * ready to paste. There is no legitimate path to writing one by hand, and the check character makes
 * sure of it: a hand-picked six characters passes only 1 time in 64, and any single-character typo
 * of a real id fails the SAME way — at compile time, rather than silently orphaning a column at
 * migration time. Use {@code datapotter-id} to mint or repair ids in bulk.
 *
 * <p><b>Optional at every granularity.</b> A field with no {@code @P} is unidentified — its renames
 * read as delete-plus-add, exactly as they do today. Nothing about adding {@code @P} to some fields
 * and not others is a half-state to warn about; it is the expected way to adopt this incrementally.
 *
 * <p><b>Retention is {@code SOURCE}, matching {@code @Data}.</b> The id therefore has no existence at
 * runtime and no reflective path back to it exists at all — it must be, and is, carried by the
 * generated field-symbol instance itself, reachable at {@link Field_I#stableId()}.
 *
 * <p>Format validated at THIS field's own declaration by the base processor: length exactly six,
 * every character in {@code A-Za-z0-9-_}, and the sixth character matching the checksum of the
 * first five. Uniqueness within the enclosing type (and, for {@code @ArcadeData}, across the whole
 * schema for a type id) is enforced the same way. None of this depends on anything ever reading the
 * field — the same "validate at the declaration" discipline {@link AsUuid} already uses.
 *
 * @see Field_I#stableId()
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface P {
    /** The six-character id: five random characters plus one check character. No default. */
    String value();
}
