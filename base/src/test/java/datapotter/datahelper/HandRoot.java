package datapotter.datahelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A hand-written {@link DataHelper_I} carrying one of every shape the deserializer has a rule for:
 * a simple field, a nested DataHelper, a list of them, a map of them, and a list of plain strings.
 * See {@link HandLeaf} for why the fixture is hand-written.
 */
public final class HandRoot implements DataHelper_I<HandRoot> {

    String id;
    HandLeaf leaf;
    List<HandLeaf> leaves;
    Map<String, HandLeaf> byName;
    List<String> notes;

    public HandRoot id(String v) { this.id = v; return this; }
    public HandRoot leaf(HandLeaf v) { this.leaf = v; return this; }
    public HandRoot leaves(List<HandLeaf> v) { this.leaves = v; return this; }
    public HandRoot byName(Map<String, HandLeaf> v) { this.byName = v; return this; }
    public HandRoot notes(List<String> v) { this.notes = v; return this; }

    public String id() { return id; }
    public HandLeaf leaf() { return leaf; }
    public List<HandLeaf> leaves() { return leaves; }
    public Map<String, HandLeaf> byName() { return byName; }
    public List<String> notes() { return notes; }

    @Override public Class<?> dataClass() { return HandRoot.class; }

    @Override
    public List<String> fieldNames() {
        return List.of("id", "leaf", "leaves", "byName", "notes");
    }

    @Override
    public Object getPropertyByName(String name) {
        return switch (name) {
            case "id" -> id;
            case "leaf" -> leaf;
            case "leaves" -> leaves;
            case "byName" -> byName;
            case "notes" -> notes;
            default -> null;
        };
    }

    @Override
    public Class<?> getPropertyType(String name) {
        return switch (name) {
            case "id" -> String.class;
            case "leaf" -> HandLeaf.class;
            case "leaves", "notes" -> List.class;
            case "byName" -> Map.class;
            default -> null;
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPropertyByName(String name, Object value) {
        switch (name) {
            case "id" -> id = (String) value;
            case "leaf" -> leaf = (HandLeaf) value;
            case "leaves" -> leaves = (List<HandLeaf>) value;
            case "byName" -> byName = (Map<String, HandLeaf>) value;
            case "notes" -> notes = (List<String>) value;
            default -> { }
        }
    }

    @Override public boolean isNestedObjectField(String name) { return "leaf".equals(name); }
    @Override public boolean isListField(String name) { return "leaves".equals(name) || "notes".equals(name); }
    @Override public boolean isMapField(String name) { return "byName".equals(name); }
    @Override public boolean isMapValueDataHelper(String name) { return "byName".equals(name); }

    @Override public Class<?> getMapKeyType(String name) { return String.class; }
    @Override public Class<?> getMapValueType(String name) { return HandLeaf.class; }

    @Override
    public DataHelper_I<?> createNestedObject(String name) {
        return "leaf".equals(name) ? new HandLeaf() : null;
    }

    @Override
    public DataHelper_I<?> createListElement(String name) {
        return "leaves".equals(name) ? new HandLeaf() : null;
    }

    @Override
    public Map<?, ?> createMapInstance(String name) {
        return "byName".equals(name) ? new LinkedHashMap<>() : null;
    }

    @Override
    public DataHelper_I<?> createMapValueElement(String name) {
        return "byName".equals(name) ? new HandLeaf() : null;
    }

    @Override public String toString() { return DataHelper_I.toString(this); }
}
