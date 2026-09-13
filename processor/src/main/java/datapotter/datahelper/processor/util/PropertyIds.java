package datapotter.datahelper.processor.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Mint, validate and repair a property/type id (PRP-28 phase 2, format settled in
 * {@code 28-prp.03.property-id-width.md}): six base64url characters, five generated at random plus
 * one check character.
 *
 * <p><b>This is the ONE implementation, shared by construction rather than by discipline.</b> The
 * base processor's {@code PropertyIdProcessor} calls it to validate every {@code @P}; the ArcadeData
 * processor calls it to validate {@code @ArcadeData(uuid=...)} and to mint the id printed in a
 * {@code requireIds} error; {@link #main} is the same logic exposed as {@code datapotter-id} for
 * bulk and scripted use. Two implementations would drift, and a drift here is a build that accepts
 * an id the other half rejects.
 *
 * <p><b>The runtime never calls into this class.</b> {@code arcadedbhelper/core} records an id as an
 * opaque string and compares by equality only (see {@code 28-prp.04} section 1) — so a future change
 * to this format is a change to this one class, not a migration of anything already stored.
 *
 * @see datapotter.datahelper.P
 */
public final class PropertyIds {

    private PropertyIds() {}

    /** RFC 4648 base64url alphabet, in the standard order: {@code value(c)} is this index. */
    public static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    public static final int ID_LENGTH = 6;
    private static final int RANDOM_LENGTH = 5;

    /** Odd weights for positions 0..4 — invertible mod 64, which is what makes a repair unique. */
    private static final int[] WEIGHTS = {1, 3, 5, 7, 9};

    /** The alphabet index of {@code c}, 0..63. */
    public static int value(char c) {
        int v = ALPHABET.indexOf(c);
        if (v < 0) throw new IllegalArgumentException(
                "Character '" + c + "' is outside the id alphabet " + ALPHABET);
        return v;
    }

    /** The alphabet character at index {@code v mod 64}. */
    public static char symbol(int v) {
        return ALPHABET.charAt(Math.floorMod(v, 64));
    }

    /**
     * The check character for exactly five characters, per the weighted-mod-64 scheme:
     * {@code (1*v1 + 3*v2 + 5*v3 + 7*v4 + 9*v5) mod 64}.
     */
    public static char checkCharFor(String five) {
        if (five == null || five.length() != RANDOM_LENGTH)
            throw new IllegalArgumentException(
                    "checkCharFor needs exactly " + RANDOM_LENGTH + " characters, got "
                            + (five == null ? "null" : five.length()));
        int sum = 0;
        for (int i = 0; i < RANDOM_LENGTH; i++) sum += WEIGHTS[i] * value(five.charAt(i));
        return symbol(sum);
    }

    /** Mint a fresh, valid six-character id. Collisions are the caller's problem (they are all loud). */
    public static String mint() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder five = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) five.append(symbol(rnd.nextInt(64)));
        return five + String.valueOf(checkCharFor(five.toString()));
    }

    public enum Problem { WRONG_LENGTH, BAD_CHARACTER, BAD_CHECK }

    /**
     * @param completedId   set only when exactly five valid characters were given: the one valid
     *                      six-character id they complete to.
     * @param repairCandidates the six single-character repairs of a bad-check id (empty otherwise).
     */
    public record ValidationResult(boolean valid, Problem problem, String message,
                                    String completedId, List<String> repairCandidates) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null, null, null, List.of());
        }
    }

    /**
     * Validate a candidate id, per the diagnostic contract in {@code 28-prp.04} section 6
     * ({@code DP-ID-001} wrong length, {@code DP-ID-002} bad character, {@code DP-ID-003} bad check).
     */
    public static ValidationResult validate(String id) {
        if (id == null) {
            return new ValidationResult(false, Problem.WRONG_LENGTH, "id is null", null, List.of());
        }
        if (id.length() == RANDOM_LENGTH) {
            for (int i = 0; i < id.length(); i++) {
                char c = id.charAt(i);
                if (ALPHABET.indexOf(c) < 0) {
                    return new ValidationResult(false, Problem.BAD_CHARACTER,
                            "Character '" + c + "' at position " + (i + 1) + " of '" + id
                                    + "' is outside A-Za-z0-9-_.", null, List.of());
                }
            }
            String completed = id + checkCharFor(id);
            return new ValidationResult(false, Problem.WRONG_LENGTH,
                    "'" + id + "' is 5 characters; a property id is 6 (5 random + 1 check character). "
                            + "Completed id: '" + completed + "'.",
                    completed, List.of(completed));
        }
        if (id.length() != ID_LENGTH) {
            return new ValidationResult(false, Problem.WRONG_LENGTH,
                    "'" + id + "' is " + id.length() + " characters; a property id is exactly "
                            + ID_LENGTH + " (5 random + 1 check character).", null, List.of());
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (ALPHABET.indexOf(c) < 0) {
                return new ValidationResult(false, Problem.BAD_CHARACTER,
                        "Character '" + c + "' at position " + (i + 1) + " of '" + id
                                + "' is outside A-Za-z0-9-_.", null, List.of());
            }
        }
        String five = id.substring(0, RANDOM_LENGTH);
        char expected = checkCharFor(five);
        char actual = id.charAt(RANDOM_LENGTH);
        if (expected != actual) {
            List<String> repairs = repairCandidates(id);
            return new ValidationResult(false, Problem.BAD_CHECK,
                    "'" + id + "' fails its check character (expected '" + expected + "', found '"
                            + actual + "'). If the first five characters are right, the id is '"
                            + (five + expected) + "'; if instead one of the first five was mistyped, "
                            + "using this id as-is orphans a column, so confirm against the committed "
                            + "id list before pasting any of the six repairs below.",
                    null, repairs);
        }
        return ValidationResult.ok();
    }

    /**
     * Every single-character repair of a six-character id, one per position, 0-indexed. Position 5
     * is the check character itself, recomputed directly; positions 0-4 are each the unique
     * replacement that makes the whole id valid again while leaving every other character untouched -
     * unique because the weights are odd and therefore invertible mod 64.
     *
     * <p><b>Never silently applied.</b> If the actual typo was in one of the first five characters,
     * "fixing" only the sixth yields a valid id matching nothing in the database — a compile error
     * quietly turned into an orphaned column, which is the one failure this whole scheme exists to
     * prevent. All six are listed so a human (or the committed id list, when one is checked) decides
     * which is real.
     */
    public static List<String> repairCandidates(String id) {
        if (id == null || id.length() != ID_LENGTH)
            throw new IllegalArgumentException("repairCandidates needs a " + ID_LENGTH + "-character id");
        List<String> candidates = new ArrayList<>(ID_LENGTH);
        for (int pos = 0; pos < ID_LENGTH; pos++) candidates.add(repairAt(id, pos));
        return candidates;
    }

    private static String repairAt(String id, int pos) {
        if (pos == RANDOM_LENGTH) {
            return id.substring(0, RANDOM_LENGTH) + checkCharFor(id.substring(0, RANDOM_LENGTH));
        }
        int weight = WEIGHTS[pos];
        int inverse = modInverse(weight, 64);
        int targetCheck = value(id.charAt(RANDOM_LENGTH));
        int sumWithoutPos = 0;
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            if (i == pos) continue;
            sumWithoutPos += WEIGHTS[i] * value(id.charAt(i));
        }
        int neededValue = Math.floorMod((targetCheck - sumWithoutPos) * inverse, 64);
        StringBuilder repaired = new StringBuilder(id);
        repaired.setCharAt(pos, symbol(neededValue));
        return repaired.toString();
    }

    private static int modInverse(int a, int m) {
        a = Math.floorMod(a, m);
        for (int x = 1; x < m; x++) if ((a * x) % m == 1) return x;
        throw new IllegalArgumentException(a + " has no inverse mod " + m);
    }

    // ===== CLI: datapotter-id new|check|fix =====

    public static void main(String[] args) {
        if (args.length == 0) { printUsage(); System.exit(2); return; }
        switch (args[0]) {
            case "new" -> {
                int n = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                for (int i = 0; i < n; i++) System.out.println(mint());
            }
            case "check" -> {
                if (args.length < 2) { printUsage(); System.exit(2); return; }
                ValidationResult r = validate(args[1]);
                if (r.valid()) System.out.println(args[1] + " is valid");
                else { System.out.println(r.message()); System.exit(1); }
            }
            case "fix" -> {
                if (args.length < 2) { printUsage(); System.exit(2); return; }
                ValidationResult r = validate(args[1]);
                if (r.valid()) { System.out.println(args[1] + " is already valid"); return; }
                if (r.repairCandidates().isEmpty()) { System.out.println(r.message()); System.exit(1); return; }
                System.out.println(r.message());
                System.out.println("Candidates, one per character position (check against the committed id list):");
                for (String c : r.repairCandidates()) System.out.println("  " + c);
            }
            default -> { printUsage(); System.exit(2); }
        }
    }

    private static void printUsage() {
        System.err.println("""
                datapotter-id new [n]        mint n ids (default 1)
                datapotter-id check <id>     valid or not, and why not
                datapotter-id fix <id>       every single-character repair""");
    }
}
