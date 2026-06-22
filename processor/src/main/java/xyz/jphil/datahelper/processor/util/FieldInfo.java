package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.TypeName;

/**
 * Information about a field discovered during annotation processing.
 * Used to pass field metadata between analysis and code generation phases.
 *
 * <p>The {@code *Generated} flags record whether a DataHelper-typed component is
 * <em>annotation-generated</em> (carries {@code @DataHelper}/{@code @Data}/&hellip;, so the
 * processor will also emit its {@code _IR}/{@code _I}/{@code _R} siblings) versus
 * <em>hand-written</em> (implements {@code DataHelper_I} directly with no generated siblings).
 * Record projection and getter widening only apply to generated components; hand-written
 * nested types degrade gracefully to their concrete type with identity conversion.</p>
 */
public class FieldInfo {
    public final String name;
    public final TypeName type;
    public final boolean isListField;
    public final boolean isNestedDataHelper;
    public final boolean isListOfDataHelper;
    public final TypeName listElementType;
    public final boolean isMapField;
    public final TypeName mapKeyType;
    public final TypeName mapValueType;
    public final boolean isMapOfDataHelper;
    public final String listImplClass;
    public final String mapImplClass;

    /** True if the nested DataHelper type is annotation-generated (has a {@code _R}/{@code _IR}). */
    public final boolean isNestedGenerated;
    /** True if the List element DataHelper type is annotation-generated. */
    public final boolean isListElementGenerated;
    /** True if the Map value DataHelper type is annotation-generated. */
    public final boolean isMapValueGenerated;

    public FieldInfo(String name, TypeName type, boolean isListField, boolean isNestedDataHelper,
                     boolean isListOfDataHelper, TypeName listElementType, boolean isMapField,
                     TypeName mapKeyType, TypeName mapValueType, boolean isMapOfDataHelper,
                     String listImplClass, String mapImplClass,
                     boolean isNestedGenerated, boolean isListElementGenerated, boolean isMapValueGenerated) {
        this.name = name;
        this.type = type;
        this.isListField = isListField;
        this.isNestedDataHelper = isNestedDataHelper;
        this.isListOfDataHelper = isListOfDataHelper;
        this.listElementType = listElementType;
        this.isMapField = isMapField;
        this.mapKeyType = mapKeyType;
        this.mapValueType = mapValueType;
        this.isMapOfDataHelper = isMapOfDataHelper;
        this.listImplClass = listImplClass;
        this.mapImplClass = mapImplClass;
        this.isNestedGenerated = isNestedGenerated;
        this.isListElementGenerated = isListElementGenerated;
        this.isMapValueGenerated = isMapValueGenerated;
    }
}
