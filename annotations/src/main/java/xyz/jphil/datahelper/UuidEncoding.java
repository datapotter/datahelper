package xyz.jphil.datahelper;

/**
 * The string encoding a {@link HasUuid} enum's 128-bit id is written in.
 *
 * <p>Bit width is fixed at 128 — not a hidden default, a fixed property of the annotation. The
 * choice this enum actually offers is the encoding alphabet, so that is what's parameterised.
 * Each value carries its fixed encoded length so {@code @AsUuid} can validate every constant's
 * literal against it at compile time.
 */
public enum UuidEncoding {
    /**
     * RFC 4648 &sect;5, alphabet {@code A-Za-z0-9-_}, unpadded. The densest form that is safe in a
     * URL, a path and a query parameter alike. The choice for new vocabularies.
     */
    BASE64URL(22, "[A-Za-z0-9_-]+"),

    /**
     * Lowercase base26 ({@code a-z}). For case-insensitive destinations: Windows filenames,
     * case-insensitive collations, anything that may lowercase a value in transit.
     */
    BASE26_LOWER(28, "[a-z]+"),

    /** Lowercase hex. For interop, where the value sits beside digests or is read as hex. */
    HEX(32, "[0-9a-f]+"),

    /**
     * The canonical 8-4-4-4-12 hyphenated UUID form, where an external system must recognise the
     * value as a UUID.
     */
    UUID_CANONICAL(36, "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final int length;
    private final String alphabetPattern;

    UuidEncoding(int length, String alphabetPattern) {
        this.length = length;
        this.alphabetPattern = alphabetPattern;
    }

    /** The fixed encoded length (in characters) of a 128-bit id under this encoding. */
    public int length() { return length; }

    /** A regex matching exactly the characters this encoding allows (no length anchor). */
    public String alphabetPattern() { return alphabetPattern; }
}
