package datapotter.datahelper;

import java.util.Collections;
import java.util.List;

/**
 * Type-safe field descriptor for a <b>reference</b> (LINK) to another DataHelper entity.
 *
 * <p>Unlike {@link DataField} — which embeds the target <em>by value</em> inside the owner —
 * a {@code LinkField} marks a field that stores only the target's identity (RID). The target
 * lives as its own record and is materialised separately (query-shaped projection or an
 * explicit resolve), never by hidden I/O. The runtime value carrier is
 * {@code datapotter.arcadedbhelper.Link<TARGET>}; this symbol is the schema/metadata side.
 *
 * <p>This is metadata only and carries no ArcadeDB dependency, mirroring {@link DataField}. The
 * {@code targetFields} list (the target type's {@code FIELDS}) lets schema registration constrain
 * the LINK to its target type without reflection.
 *
 * @param <PARENT> the entity declaring the reference
 * @param <TARGET> the referenced (linked) DataHelper entity type
 */
public final class LinkField<PARENT extends DataHelper_I<PARENT>,
                             TARGET extends DataHelper_I<TARGET>>
        implements Field_I<PARENT, TARGET> {

    private final String name;
    private final Class<TARGET> type;
    private final List<Field_I<TARGET, ?>> targetFields;
    private final String stableId;

    /**
     * @param name         the field name
     * @param type         the linked (target) entity type
     * @param targetFields the target type's static FIELDS list (e.g. {@code Customer_A.FIELDS})
     */
    public LinkField(String name, Class<TARGET> type, List<Field_I<TARGET, ?>> targetFields) {
        this(name, type, targetFields, null);
    }

    /** @param stableId the field's {@code @P} value (PRP-28 phase 2), or {@code null} if unidentified. */
    public LinkField(String name, Class<TARGET> type, List<Field_I<TARGET, ?>> targetFields, String stableId) {
        this.name = name;
        this.type = type;
        this.targetFields = targetFields;
        this.stableId = stableId;
        this.__ = name();
    }

    /** Legacy/simple constructor without the target FIELDS reference. */
    public LinkField(String name, Class<TARGET> type) {
        this(name, type, Collections.emptyList());
    }

    @Override public String name() { return name; }

    /** The <em>target</em> entity type (the thing linked to), e.g. {@code Customer.class}. */
    @Override public Class<TARGET> type() { return type; }
    @Override public String stableId() { return stableId; }

    /** The target type's FIELDS list, for zero-reflection schema constraint / projection. */
    public List<Field_I<TARGET, ?>> targetFields() { return targetFields; }

    /** Field name, mirroring the {@code __} convention on the other symbol types. */
    public final String __;

    /**
     * The value held in the owner is a {@code Link<TARGET>} wrapper, not a {@code TARGET}, so the
     * inherited instance-of check would wrongly reject it. References are validated structurally
     * elsewhere; accept any non-null value here.
     */
    @Override public boolean validate(Object value) { return true; }

    /**
     * Chain to a simple field of the target for a dotted projection path (e.g. {@code customer.name}).
     * Useful when building nested-projection field references.
     */
    public <SUB> Field<PARENT, SUB> __(Field<TARGET, SUB> subField) {
        return new Field<>(this.name + "." + subField.name(), subField.type());
    }

    /** Chain to a nested DataField of the target for a deeper projection path. */
    public <DEEPER extends DataHelper_I<DEEPER>>
    DataField<PARENT, DEEPER> ___(DataField<TARGET, DEEPER> subField) {
        return new DataField<>(this.name + "." + subField.name(), subField.type());
    }

    public static <PARENT extends DataHelper_I<PARENT>, TARGET extends DataHelper_I<TARGET>>
    LinkField<PARENT, TARGET> of(String name, Class<TARGET> type) {
        return new LinkField<>(name, type);
    }

    @Override
    public String toString() {
        return "LinkField[" + name + " -> " + type.getSimpleName() + "]";
    }
}
