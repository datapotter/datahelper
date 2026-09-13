package datapotter.datahelper;

import java.util.Collections;
import java.util.List;

/**
 * Type-safe field descriptor for a <b>list of references</b> (LIST of LINK) to a DataHelper entity.
 *
 * <p>The list-of-links analogue of {@link LinkField}: a 1:n / n:n reference where the owner stores
 * a list of target RIDs, not embedded copies (contrast {@link ListDataField}, which embeds). The
 * runtime value carrier is {@code datapotter.arcadedbhelper.LinkList<TARGET>}; this symbol is
 * the schema/metadata side and carries no ArcadeDB dependency.
 *
 * @param <PARENT> the entity declaring the reference list
 * @param <TARGET> the referenced (linked) element entity type
 */
public final class LinkListField<PARENT extends DataHelper_I<PARENT>,
                                 TARGET extends DataHelper_I<TARGET>>
        implements Field_I<PARENT, List<TARGET>> {

    private final String name;
    private final Class<TARGET> elementType;
    private final List<Field_I<TARGET, ?>> elementFields;
    private final String stableId;

    public LinkListField(String name, Class<TARGET> elementType, List<Field_I<TARGET, ?>> elementFields) {
        this(name, elementType, elementFields, null);
    }

    /** @param stableId the field's {@code @P} value (PRP-28 phase 2), or {@code null} if unidentified. */
    public LinkListField(String name, Class<TARGET> elementType, List<Field_I<TARGET, ?>> elementFields,
                          String stableId) {
        this.name = name;
        this.elementType = elementType;
        this.elementFields = elementFields;
        this.stableId = stableId;
        this.__ = name();
    }

    public LinkListField(String name, Class<TARGET> elementType) {
        this(name, elementType, Collections.emptyList());
    }

    @Override public String name() { return name; }
    @Override public String stableId() { return stableId; }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<TARGET>> type() {
        return (Class<List<TARGET>>) (Class<?>) List.class;
    }

    /** The linked element (target) entity type, e.g. {@code LineItem.class}. */
    public Class<TARGET> elementType() { return elementType; }

    /** The element type's FIELDS list, for zero-reflection schema constraint / projection. */
    public List<Field_I<TARGET, ?>> elementFields() { return elementFields; }

    public final String __;

    @Override public boolean validate(Object value) { return true; }

    public static <PARENT extends DataHelper_I<PARENT>, TARGET extends DataHelper_I<TARGET>>
    LinkListField<PARENT, TARGET> of(String name, Class<TARGET> elementType) {
        return new LinkListField<>(name, elementType);
    }

    @Override
    public String toString() {
        return "LinkListField[" + name + " -> List<" + elementType.getSimpleName() + ">]";
    }
}
