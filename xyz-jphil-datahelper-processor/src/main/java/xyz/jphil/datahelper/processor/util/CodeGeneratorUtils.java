package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.*;
import xyz.jphil.datahelper.DataField;
import xyz.jphil.datahelper.Field;
import xyz.jphil.datahelper.Field_I;
import xyz.jphil.datahelper.ListDataField;
import xyz.jphil.datahelper.MapDataField;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Utility class for generating common JavaPoet code patterns.
 * Used across multiple annotation processors for DataHelper generation.
 */
public class CodeGeneratorUtils {

    /**
     * Returns the correct modifiers for a method with an implementation body.
     * Interface methods with bodies need {@code default}; class methods just need {@code public}.
     */
    private static Modifier[] implModifiers(boolean isInterface) {
        return isInterface
                ? new Modifier[]{Modifier.PUBLIC, Modifier.DEFAULT}
                : new Modifier[]{Modifier.PUBLIC};
    }

    /**
     * Extracts the raw type from a TypeName.
     * For parameterized types like List<String>, returns List.
     * For simple types like String, returns String.
     */
    private static TypeName getRawType(TypeName typeName) {
        if (typeName instanceof ParameterizedTypeName) {
            return ((ParameterizedTypeName) typeName).rawType();
        }
        return typeName;
    }

    /**
     * Generate Field symbol constants ($fieldName) for all fields.
     * Uses Field for simple types, DataField for nested DataHelper types.
     */
    public static void addFieldSymbols(TypeSpec.Builder builder, List<FieldInfo> fields,
                                       String packageName, String className) {
        for (FieldInfo field : fields) {
            String symbolName = "$" + field.name;

            // Box primitive types (int -> Integer, double -> Double, etc.)
            TypeName boxedFieldType = field.type.isPrimitive() ? field.type.box() : field.type;

            // Check if this field is a nested DataHelper type
            boolean isNestedDataHelper = field.isNestedDataHelper;

            // Get raw type for .class literal (List.class instead of List<String>.class)
            TypeName rawFieldType = getRawType(boxedFieldType);

            if (isNestedDataHelper) {
                // Use DataField for nested DataHelper types (supports __() chaining)
                // Pass the nested type's FIELDS list
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(DataField.class),
                    ClassName.get(packageName, className),
                    boxedFieldType
                );

                // Get nested type's class name for FIELDS reference
                String nestedClassName = rawFieldType.toString();
                if (nestedClassName.contains(".")) {
                    nestedClassName = nestedClassName.substring(nestedClassName.lastIndexOf('.') + 1);
                }

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $L_A.FIELDS)",
                            ClassName.get(DataField.class),
                            field.name,
                            rawFieldType,
                            nestedClassName)
                        .build();
                builder.addField(symbol);
            } else if (field.isListOfDataHelper) {
                // Use ListDataField for List<DataHelper> types
                TypeName elementType = field.listElementType;
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(ListDataField.class),
                    ClassName.get(packageName, className),
                    elementType
                );

                // Get element type's class name for FIELDS reference
                String elementClassName = elementType.toString();
                if (elementClassName.contains(".")) {
                    elementClassName = elementClassName.substring(elementClassName.lastIndexOf('.') + 1);
                }

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $L_A.FIELDS)",
                            ClassName.get(ListDataField.class),
                            field.name,
                            elementType,
                            elementClassName)
                        .build();
                builder.addField(symbol);
            } else if (field.isMapOfDataHelper) {
                // Use MapDataField for Map<K, DataHelper> types
                TypeName keyType = field.mapKeyType;
                TypeName valueType = field.mapValueType;
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(MapDataField.class),
                    ClassName.get(packageName, className),
                    keyType,
                    valueType
                );

                // Get value type's class name for FIELDS reference
                String valueClassName = valueType.toString();
                if (valueClassName.contains(".")) {
                    valueClassName = valueClassName.substring(valueClassName.lastIndexOf('.') + 1);
                }

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $T.class, $L_A.FIELDS)",
                            ClassName.get(MapDataField.class),
                            field.name,
                            keyType,
                            valueType,
                            valueClassName)
                        .build();
                builder.addField(symbol);
            } else {
                // Use regular Field for simple types
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(Field.class),
                    ClassName.get(packageName, className),
                    boxedFieldType
                );

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class)",
                            ClassName.get(Field.class),
                            field.name,
                            rawFieldType)
                        .build();
                builder.addField(symbol);
            }
        }
    }

    /**
     * Generate immutable FIELDS list: List<Field_I<EntityType, ?>>
     */
    public static void addFieldsList(TypeSpec.Builder builder, List<FieldInfo> fields,
                                     String packageName, String className) {
        CodeBlock.Builder fieldsListInitBuilder = CodeBlock.builder().add("$T.of(", List.class);
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                fieldsListInitBuilder.add(", ");
            }
            fieldsListInitBuilder.add("$N", "$" + fields.get(i).name);
        }
        fieldsListInitBuilder.add(")");

        // Type: List<Field_I<EntityType, ?>>
        ParameterizedTypeName fieldsListType = ParameterizedTypeName.get(
            ClassName.get(List.class),
            ParameterizedTypeName.get(
                ClassName.get(Field_I.class),
                ClassName.get(packageName, className),
                WildcardTypeName.subtypeOf(Object.class)
            )
        );

        FieldSpec fieldsListField = FieldSpec.builder(fieldsListType, "FIELDS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(fieldsListInitBuilder.build())
                .build();
        builder.addField(fieldsListField);
    }

    /**
     * Generate getPropertyByName(String) method using switch expression.
     */
    public static MethodSpec createGetPropertyByNameMethod(List<FieldInfo> fields, ProcessorUtils utils, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getPropertyByName")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(Object.class);

        if (fields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            // Build switch expression as a statement (with semicolon)
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : fields) {
                // Use "is" prefix for boolean types (primitive boolean and Boolean wrapper)
                String prefix = ProcessorUtils.isBooleanType(field.type) ? "is" : "get";
                String getterName = prefix + ProcessorUtils.capitalize(field.name);
                switchBlock.add("case $S -> $N();\n", field.name, getterName);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate setPropertyByName(String, Object) method using switch statement.
     */
    public static MethodSpec createSetPropertyByNameMethod(List<FieldInfo> fields, ProcessorUtils utils, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("setPropertyByName")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addParameter(String.class, "propertyName")
                .addParameter(Object.class, "value");

        if (!fields.isEmpty()) {
            builder.beginControlFlow("switch (propertyName)");
            for (FieldInfo field : fields) {
                String setterName = "set" + ProcessorUtils.capitalize(field.name);
                // Use convertType for primitives and wrapper types
                if (field.type.isPrimitive() || ProcessorUtils.isWrapperType(field.type)) {
                    TypeName wrapperType = field.type.isPrimitive() ? field.type.box() : field.type;
                    builder.addStatement("case $S -> $N(($T) $T.convertType(value, $T.class))",
                            field.name, setterName, field.type,
                            ClassName.get("xyz.jphil.datahelper", "DataHelper_I"), wrapperType);
                } else {
                    builder.addStatement("case $S -> $N(($T) value)", field.name, setterName, field.type);
                }
            }
            builder.endControlFlow();
        }

        return builder.build();
    }

    /**
     * Generate getPropertyType(String) method.
     */
    public static MethodSpec createGetPropertyTypeMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getPropertyType")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        if (fields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : fields) {
                // Box primitive types and get raw type for .class literal
                TypeName boxedType = field.type.isPrimitive() ? field.type.box() : field.type;
                TypeName rawType = getRawType(boxedType);
                switchBlock.add("case $S -> $T.class;\n", field.name, rawType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate fieldNames() method.
     */
    public static MethodSpec createFieldNamesMethod(boolean isInterface) {
        return MethodSpec.methodBuilder("fieldNames")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)))
                .addStatement("return FIELDS.stream().map($T::name).toList()",
                    ClassName.get(Field_I.class))
                .build();
    }

    /**
     * Generate dataClass() method.
     */
    public static MethodSpec createDataClassMethod(String packageName, String className, boolean isInterface) {
        return MethodSpec.methodBuilder("dataClass")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("return $T.class", ClassName.get(packageName, className))
                .build();
    }

    /**
     * Generate createNestedObject(String) method.
     */
    public static MethodSpec createNestedObjectMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createNestedObject")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> nestedFields = fields.stream().filter(f -> f.isNestedDataHelper).toList();
        if (nestedFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : nestedFields) {
                switchBlock.add("case $S -> new $T();\n", field.name, field.type);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate createListElement(String) method.
     */
    public static MethodSpec createListElementMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createListElement")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> listOfDataHelperFields = fields.stream().filter(f -> f.isListOfDataHelper).toList();
        if (listOfDataHelperFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : listOfDataHelperFields) {
                switchBlock.add("case $S -> new $T();\n", field.name, field.listElementType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate isListField(String) method.
     */
    public static MethodSpec createIsListFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("isListField")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> listFields = fields.stream().filter(f -> f.isListField).toList();
        if (listFields.isEmpty()) {
            builder.addStatement("return false");
        } else if (listFields.size() == 1) {
            builder.addStatement("return $S.equals(propertyName)", listFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : listFields) {
                switchBlock.add("case $S -> true;\n", field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate isNestedObjectField(String) method.
     */
    public static MethodSpec createIsNestedObjectFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("isNestedObjectField")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> nestedFields = fields.stream().filter(f -> f.isNestedDataHelper).toList();
        if (nestedFields.isEmpty()) {
            builder.addStatement("return false");
        } else if (nestedFields.size() == 1) {
            builder.addStatement("return $S.equals(propertyName)", nestedFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : nestedFields) {
                switchBlock.add("case $S -> true;\n", field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    /**
     * Generate Map-related methods (6 methods for Map support).
     */
    public static void addMapMethods(TypeSpec.Builder builder, List<FieldInfo> fields, boolean isInterface) {
        builder.addMethod(createIsMapFieldMethod(fields, isInterface));
        builder.addMethod(createGetMapKeyTypeMethod(fields, isInterface));
        builder.addMethod(createGetMapValueTypeMethod(fields, isInterface));
        builder.addMethod(createCreateMapInstanceMethod(fields, isInterface));
        builder.addMethod(createIsMapValueDataHelperMethod(fields, isInterface));
        builder.addMethod(createCreateMapValueElementMethod(fields, isInterface));
    }

    private static MethodSpec createIsMapFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("isMapField")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> mapFields = fields.stream().filter(f -> f.isMapField).toList();
        if (mapFields.isEmpty()) {
            builder.addStatement("return false");
        } else if (mapFields.size() == 1) {
            builder.addStatement("return $S.equals(propertyName)", mapFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                switchBlock.add("case $S -> true;\n", field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    private static MethodSpec createGetMapKeyTypeMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getMapKeyType")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> mapFields = fields.stream().filter(f -> f.isMapField).toList();
        if (mapFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                switchBlock.add("case $S -> $T.class;\n", field.name, field.mapKeyType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    private static MethodSpec createGetMapValueTypeMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getMapValueType")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> mapFields = fields.stream().filter(f -> f.isMapField).toList();
        if (mapFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                switchBlock.add("case $S -> $T.class;\n", field.name, field.mapValueType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    private static MethodSpec createCreateMapInstanceMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createMapInstance")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        WildcardTypeName.subtypeOf(Object.class),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> mapFields = fields.stream().filter(f -> f.isMapField).toList();
        if (mapFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                String implClass = field.mapImplClass != null ? field.mapImplClass : "java.util.LinkedHashMap";
                switchBlock.add("case $S -> new $L<>();\n", field.name, implClass);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    private static MethodSpec createIsMapValueDataHelperMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("isMapValueDataHelper")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> mapOfDataHelperFields = fields.stream().filter(f -> f.isMapOfDataHelper).toList();
        if (mapOfDataHelperFields.isEmpty()) {
            builder.addStatement("return false");
        } else if (mapOfDataHelperFields.size() == 1) {
            builder.addStatement("return $S.equals(propertyName)", mapOfDataHelperFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapOfDataHelperFields) {
                switchBlock.add("case $S -> true;\n", field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }

    private static MethodSpec createCreateMapValueElementMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createMapValueElement")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> mapOfDataHelperFields = fields.stream().filter(f -> f.isMapOfDataHelper).toList();
        if (mapOfDataHelperFields.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapOfDataHelperFields) {
                switchBlock.add("case $S -> new $T();\n", field.name, field.mapValueType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");
            builder.addCode(switchBlock.build());
        }

        return builder.build();
    }
}
