package datapotter.datahelper;

import java.util.List;

/**
 * A hand-written {@link DataHelper_I} with no generated code behind it, used by
 * {@link MapRoundTripCheck}.
 *
 * <p>Its point is what is NOT here: no annotation processor, no other module, nothing but this jar.
 * {@link MapReads} and {@link MapWrites} are grounded on this contract alone, and a fixture written
 * by hand is the only way to say so without importing the thing being claimed unnecessary.</p>
 */
public final class HandLeaf implements DataHelper_I<HandLeaf> {

    String sha256;
    Integer bytes;

    public HandLeaf sha256(String v) { this.sha256 = v; return this; }
    public HandLeaf bytes(Integer v) { this.bytes = v; return this; }

    public String sha256() { return sha256; }
    public Integer bytes() { return bytes; }

    @Override public Class<?> dataClass() { return HandLeaf.class; }
    @Override public List<String> fieldNames() { return List.of("sha256", "bytes"); }

    @Override
    public Object getPropertyByName(String name) {
        return switch (name) {
            case "sha256" -> sha256;
            case "bytes" -> bytes;
            default -> null;
        };
    }

    @Override
    public Class<?> getPropertyType(String name) {
        return switch (name) {
            case "sha256" -> String.class;
            case "bytes" -> Integer.class;
            default -> null;
        };
    }

    @Override
    public void setPropertyByName(String name, Object value) {
        switch (name) {
            case "sha256" -> sha256 = (String) value;
            case "bytes" -> bytes = (Integer) DataHelper_I.convertType(value, Integer.class);
            default -> { }
        }
    }

    @Override public boolean isListField(String name) { return false; }
    @Override public boolean isNestedObjectField(String name) { return false; }
    @Override public boolean isMapField(String name) { return false; }
    @Override public boolean isMapValueDataHelper(String name) { return false; }

    @Override public String toString() { return DataHelper_I.toString(this); }
}
