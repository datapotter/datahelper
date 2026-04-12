package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.TypeName;

/**
 * Information about a field discovered during annotation processing.
 * Used to pass field metadata between analysis and code generation phases.
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

    public FieldInfo(String name, TypeName type, boolean isListField, boolean isNestedDataHelper,
                     boolean isListOfDataHelper, TypeName listElementType, boolean isMapField,
                     TypeName mapKeyType, TypeName mapValueType, boolean isMapOfDataHelper,
                     String listImplClass, String mapImplClass) {
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
    }
}
