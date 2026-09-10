package datapotter.datahelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PRP-29: {@link MapReads} / {@link MapWrites} round-trip a DataHelper through a plain map with this
 * jar as the only thing on the classpath &mdash; no annotation processor, no backend, no database.
 *
 * <p>That is the claim the PRP is built on: nothing in the walk was ever backend-specific, and the
 * defect it fixes existed only because the walk was living on a backend trait an embedded block
 * cannot implement. A test that needed a backend present to run could not make the claim.</p>
 *
 * <p>Run with {@code mvn -o exec:java
 * -Dexec.mainClass=datapotter.datahelper.MapRoundTripCheck -Dexec.classpathScope=test}.
 * The generated-code counterpart is {@code test-scripts/data-map-test}.</p>
 */
public final class MapRoundTripCheck {

    public static void main(String[] args) {
        var bad = new ArrayList<String>();

        var written = sample().toMap();
        System.out.println("toMap  : " + written);
        checkShape(written, bad);
        check(new HandRoot().fromMap(written), bad);
        checkPartialIsPartial(bad);
        checkLoudFailure(bad);

        System.out.println(bad.isEmpty() ? "ok" : bad.size() + " failure(s): " + String.join("; ", bad));
        if (!bad.isEmpty()) System.exit(1);
    }

    private static HandRoot sample() {
        return new HandRoot()
                .id("r-1")
                .leaf(new HandLeaf().sha256("abc123").bytes(4096))
                .leaves(List.of(new HandLeaf().sha256("def456").bytes(8)))
                .byName(Map.of("cover", new HandLeaf().sha256("ff00").bytes(1)))
                .notes(List.of("first", "second"));
    }

    /** Nested DataHelpers must come out as nested maps, at every position. */
    private static void checkShape(Map<String, Object> m, List<String> bad) {
        if (!(m.get("leaf") instanceof Map)) bad.add("toMap: leaf = " + describe(m.get("leaf")));
        if (!(m.get("leaves") instanceof List<?> l) || !(l.get(0) instanceof Map)) {
            bad.add("toMap: leaves = " + describe(m.get("leaves")));
        }
        if (!(m.get("byName") instanceof Map<?, ?> b) || !(b.get("cover") instanceof Map)) {
            bad.add("toMap: byName = " + describe(m.get("byName")));
        }
        if (!(m.get("notes") instanceof List<?> n) || !(n.get(0) instanceof String)) {
            bad.add("toMap: notes = " + describe(m.get("notes")));
        }
    }

    private static void check(HandRoot r, List<String> bad) {
        if (!"r-1".equals(r.id())) bad.add("id = " + r.id());

        if (r.leaf() == null) bad.add("leaf is null");
        else if (!"abc123".equals(r.leaf().sha256()) || !Integer.valueOf(4096).equals(r.leaf().bytes())) {
            bad.add("leaf = " + r.leaf());
        }

        var leaves = (List<?>) r.leaves();
        if (leaves == null || leaves.size() != 1) bad.add("leaves = " + leaves);
        else if (!(leaves.get(0) instanceof HandLeaf first)) {
            bad.add("leaves[0] is a " + leaves.get(0).getClass().getName() + ", not a HandLeaf");
        } else if (!"def456".equals(first.sha256()) || !Integer.valueOf(8).equals(first.bytes())) {
            bad.add("leaves[0] = " + first);
        }

        var byName = (Map<?, ?>) r.byName();
        if (byName == null || byName.size() != 1) bad.add("byName = " + byName);
        else if (!(byName.get("cover") instanceof HandLeaf cover)) {
            bad.add("byName[cover] is a " + describe(byName.get("cover")) + ", not a HandLeaf");
        } else if (!"ff00".equals(cover.sha256()) || !Integer.valueOf(1).equals(cover.bytes())) {
            bad.add("byName[cover] = " + cover);
        }

        if (r.notes() == null || r.notes().size() != 2 || !"first".equals(r.notes().get(0))) {
            bad.add("notes = " + r.notes());
        }
    }

    /** A map missing a key leaves that field alone, so a projection updates rather than erases. */
    private static void checkPartialIsPartial(List<String> bad) {
        var r = sample();
        r.fromMap(Map.of("id", "r-2"));
        if (!"r-2".equals(r.id())) bad.add("partial: id = " + r.id());
        if (r.leaf() == null) bad.add("partial: leaf was erased by a map that did not mention it");
    }

    /** A record-shaped value in a field that cannot hold one fails here, naming the field. */
    private static void checkLoudFailure(List<String> bad) {
        try {
            new HandRoot().fromMap(Map.of("notes", List.of(Map.of("oops", 1))));
            bad.add("loud-failure: a map inside List<String> was accepted silently");
        } catch (IllegalStateException e) {
            if (!e.getMessage().contains("notes")) {
                bad.add("loud-failure: message does not name the field: " + e.getMessage());
            } else {
                System.out.println("loud   : " + e.getMessage());
            }
        }
    }

    private static String describe(Object o) {
        return o == null ? "null" : o.getClass().getName() + " " + o;
    }
}
