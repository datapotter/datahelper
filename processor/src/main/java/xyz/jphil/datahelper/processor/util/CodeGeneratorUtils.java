package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.*;
import xyz.jphil.datahelper.DataField;
import xyz.jphil.datahelper.Field;
import xyz.jphil.datahelper.Field_I;
import xyz.jphil.datahelper.LinkField;
import xyz.jphil.datahelper.LinkListField;
import xyz.jphil.datahelper.LinkMapField;
import xyz.jphil.datahelper.ListDataField;
import xyz.jphil.datahelper.MapDataField;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
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
     * Reference to a nested DataHelper type's generated {@code FIELDS} list, e.g. {@code Address_A.FIELDS}.
     * Emitted as a {@code $T} so JavaPoet imports the companion when it lives in another package;
     * an unresolvable type degrades to the bare simple name, which is what a same-round sibling needs.
     */
    private static CodeBlock fieldsRef(TypeName concrete, String fieldsHostSuffix) {
        var raw = getRawType(concrete);
        return raw instanceof ClassName cn
                ? CodeBlock.of("$T.FIELDS", ClassName.get(cn.packageName(), cn.simpleName() + fieldsHostSuffix))
                : CodeBlock.of("$L$L.FIELDS", simpleName(raw), fieldsHostSuffix);
    }

    /**
     * Generate Field symbol constants ($fieldName) for all fields.
     * Uses Field for simple types, DataField/ListDataField/MapDataField for nested DataHelper types.
     *
     * @param fieldsHostSuffix the suffix of the sibling type that hosts a nested type's {@code FIELDS}
     *                         list (e.g. {@code "_IR"} for the @DataHelper/@Data projection, {@code "_A"}
     *                         for the @ArcadeData sealed-base path).
     */
    public static void addFieldSymbols(TypeSpec.Builder builder, List<FieldInfo> fields,
                                       String packageName, String className, String fieldsHostSuffix) {
        for (FieldInfo field : fields) {
            String symbolName = "$" + field.name;

            // Box primitive types (int -> Integer, double -> Double, etc.)
            TypeName boxedFieldType = field.type.isPrimitive() ? field.type.box() : field.type;

            // Get raw type for .class literal (List.class instead of List<String>.class)
            TypeName rawFieldType = getRawType(boxedFieldType);

            // Type-safe chaining symbols (DataField/ListDataField/MapDataField) reference the
            // nested type's generated FIELDS list, which lives on its _IR interface. This is only
            // available for annotation-generated nested types; hand-written DataHelpers degrade to
            // a plain Field (no chaining) since they have no generated _IR.
            if (field.isNestedDataHelper && field.isNestedGenerated) {
                // Use DataField for nested DataHelper types (supports __() chaining)
                // Pass the nested type's FIELDS list
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(DataField.class),
                    ClassName.get(packageName, className),
                    boxedFieldType
                );

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $L)",
                            ClassName.get(DataField.class),
                            field.name,
                            rawFieldType,
                            fieldsRef(rawFieldType, fieldsHostSuffix))
                        .build();
                builder.addField(symbol);
            } else if (field.isListOfDataHelper && field.isListElementGenerated) {
                // Use ListDataField for List<DataHelper> types
                TypeName elementType = field.listElementType;
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(ListDataField.class),
                    ClassName.get(packageName, className),
                    elementType
                );

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $L)",
                            ClassName.get(ListDataField.class),
                            field.name,
                            elementType,
                            fieldsRef(elementType, fieldsHostSuffix))
                        .build();
                builder.addField(symbol);
            } else if (field.isMapOfDataHelper && field.isMapValueGenerated) {
                // Use MapDataField for Map<K, DataHelper> types
                TypeName keyType = field.mapKeyType;
                TypeName valueType = field.mapValueType;
                TypeName fieldGenericType = ParameterizedTypeName.get(
                    ClassName.get(MapDataField.class),
                    ClassName.get(packageName, className),
                    keyType,
                    valueType
                );

                FieldSpec symbol = FieldSpec.builder(fieldGenericType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($S, $T.class, $T.class, $L)",
                            ClassName.get(MapDataField.class),
                            field.name,
                            keyType,
                            valueType,
                            fieldsRef(valueType, fieldsHostSuffix))
                        .build();
                builder.addField(symbol);
            } else if (field.isLink) {
                // Reference (LINK): symbol is LinkField<Owner, Target>; value carrier is Link<Target>.
                TypeName target = getRawType(field.linkTargetType);
                TypeName symType = ParameterizedTypeName.get(
                    ClassName.get(LinkField.class), ClassName.get(packageName, className), field.linkTargetType);
                FieldSpec.Builder sb = FieldSpec.builder(symType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
                if (field.isLinkTargetGenerated) {
                    sb.initializer("new $T($S, $T.class, $L)",
                        ClassName.get(LinkField.class), field.name, target, fieldsRef(target, fieldsHostSuffix));
                } else {
                    sb.initializer("new $T($S, $T.class)", ClassName.get(LinkField.class), field.name, target);
                }
                builder.addField(sb.build());
            } else if (field.isLinkList) {
                // List of references (LIST of LINK): LinkListField<Owner, Element>.
                TypeName element = getRawType(field.linkTargetType);
                TypeName symType = ParameterizedTypeName.get(
                    ClassName.get(LinkListField.class), ClassName.get(packageName, className), field.linkTargetType);
                FieldSpec.Builder sb = FieldSpec.builder(symType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
                if (field.isLinkTargetGenerated) {
                    sb.initializer("new $T($S, $T.class, $L)",
                        ClassName.get(LinkListField.class), field.name, element, fieldsRef(element, fieldsHostSuffix));
                } else {
                    sb.initializer("new $T($S, $T.class)", ClassName.get(LinkListField.class), field.name, element);
                }
                builder.addField(sb.build());
            } else if (field.isLinkMap) {
                // Keyed map of references (MAP of LINK): LinkMapField<Owner, Key, Value>.
                TypeName key = field.linkMapKeyType;
                TypeName value = getRawType(field.linkTargetType);
                TypeName symType = ParameterizedTypeName.get(
                    ClassName.get(LinkMapField.class), ClassName.get(packageName, className), key, field.linkTargetType);
                FieldSpec.Builder sb = FieldSpec.builder(symType, symbolName,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
                if (field.isLinkTargetGenerated) {
                    sb.initializer("new $T($S, $T.class, $T.class, $L)",
                        ClassName.get(LinkMapField.class), field.name, key, value, fieldsRef(value, fieldsHostSuffix));
                } else {
                    sb.initializer("new $T($S, $T.class, $T.class)",
                        ClassName.get(LinkMapField.class), field.name, key, value);
                }
                builder.addField(sb.build());
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
     * Generate {@code Object.equals(Object)} delegating to {@code DataHelper_I.equals(this, o)}.
     *
     * <p>Class-only: interfaces cannot override Object methods, so this is used by the
     * {@code @Data} ({@code _A}) generator. The {@code @DataHelper} ({@code _I}) path relies
     * on Lombok for equals/hashCode/toString.</p>
     */
    public static MethodSpec createEqualsMethod() {
        return MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(Object.class, "o")
                .returns(boolean.class)
                .addStatement("return $T.equals(this, o)",
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"))
                .build();
    }

    /**
     * Generate {@code Object.hashCode()} delegating to {@code DataHelper_I.hashCode(this)}.
     */
    public static MethodSpec createHashCodeMethod() {
        return MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(int.class)
                .addStatement("return $T.hashCode(this)",
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"))
                .build();
    }

    /**
     * Generate {@code Object.toString()} delegating to {@code DataHelper_I.toString(this)}.
     */
    public static MethodSpec createToStringMethod() {
        return MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $T.toString(this)",
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"))
                .build();
    }

    /**
     * Generate the <em>read-side</em> Map metadata methods (belong on the readable {@code _IR}):
     * {@code isMapField}, {@code getMapKeyType}, {@code getMapValueType}, {@code isMapValueDataHelper}.
     */
    public static void addMapReadMethods(TypeSpec.Builder builder, List<FieldInfo> fields, boolean isInterface) {
        builder.addMethod(createIsMapFieldMethod(fields, isInterface));
        builder.addMethod(createGetMapKeyTypeMethod(fields, isInterface));
        builder.addMethod(createGetMapValueTypeMethod(fields, isInterface));
        builder.addMethod(createIsMapValueDataHelperMethod(fields, isInterface));
    }

    /**
     * Generate the <em>write-side</em> Map factory methods (belong on the writable {@code _I}):
     * {@code createMapInstance}, {@code createMapValueElement}.
     */
    public static void addMapWriteMethods(TypeSpec.Builder builder, List<FieldInfo> fields, boolean isInterface) {
        builder.addMethod(createCreateMapInstanceMethod(fields, isInterface));
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

    // ========== Reference (LINK) metadata generation (override ArcadeDoc_I defaults) ==========

    /** Best-effort simple class name from a TypeName (strips package and any type arguments). */
    private static String simpleName(TypeName t) {
        String s = t.toString();
        int lt = s.indexOf('<');
        if (lt >= 0) s = s.substring(0, lt);
        int dot = s.lastIndexOf('.');
        if (dot >= 0) s = s.substring(dot + 1);
        return s;
    }

    /** Shared generator for a {@code boolean field-name predicate} method (case name -> true). */
    private static MethodSpec booleanFieldPredicate(String methodName, List<FieldInfo> fields,
            java.util.function.Predicate<FieldInfo> predicate, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> matched = fields.stream().filter(predicate).toList();
        if (matched.isEmpty()) {
            builder.addStatement("return false");
        } else if (matched.size() == 1) {
            builder.addStatement("return $S.equals(propertyName)", matched.get(0).name);
        } else {
            CodeBlock.Builder sw = CodeBlock.builder();
            sw.add("return switch (propertyName) {\n").indent();
            for (FieldInfo f : matched) sw.add("case $S -> true;\n", f.name);
            sw.add("default -> false;\n").unindent().add("};");
            builder.addCode(sw.build());
        }
        return builder.build();
    }

    public static MethodSpec createIsLinkFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        return booleanFieldPredicate("isLinkField", fields, f -> f.isLink, isInterface);
    }

    public static MethodSpec createIsLinkListFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        return booleanFieldPredicate("isLinkListField", fields, f -> f.isLinkList, isInterface);
    }

    public static MethodSpec createIsLinkMapFieldMethod(List<FieldInfo> fields, boolean isInterface) {
        return booleanFieldPredicate("isLinkMapField", fields, f -> f.isLinkMap, isInterface);
    }

    /** {@code Class<?> linkTargetType(String)} — the linked target type for any reference field. */
    public static MethodSpec createLinkTargetTypeMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("linkTargetType")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> links = fields.stream().filter(FieldInfo::isAnyLink).toList();
        if (links.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder sw = CodeBlock.builder();
            sw.add("return switch (propertyName) {\n").indent();
            for (FieldInfo f : links) sw.add("case $S -> $T.class;\n", f.name, getRawType(f.linkTargetType));
            sw.add("default -> null;\n").unindent().add("};");
            builder.addCode(sw.build());
        }
        return builder.build();
    }

    /** {@code Class<?> linkKeyType(String)} — the key type for LinkMap reference fields. */
    public static MethodSpec createLinkKeyTypeMethod(List<FieldInfo> fields, boolean isInterface) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("linkKeyType")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> mapLinks = fields.stream().filter(f -> f.isLinkMap).toList();
        if (mapLinks.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder sw = CodeBlock.builder();
            sw.add("return switch (propertyName) {\n").indent();
            for (FieldInfo f : mapLinks) sw.add("case $S -> $T.class;\n", f.name, getRawType(f.linkMapKeyType));
            sw.add("default -> null;\n").unindent().add("};");
            builder.addCode(sw.build());
        }
        return builder.build();
    }

    /** {@code ArcadeDoc_I<?> createLinkTarget(String)} — fresh target instance to populate a projection. */
    public static MethodSpec createLinkTargetFactoryMethod(List<FieldInfo> fields, boolean isInterface) {
        ClassName arcadeDoc = ClassName.get("xyz.jphil.arcadedb.datahelper", "ArcadeDoc_I");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createLinkTarget")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(arcadeDoc, WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> links = fields.stream().filter(FieldInfo::isAnyLink).toList();
        if (links.isEmpty()) {
            builder.addStatement("return null");
        } else {
            CodeBlock.Builder sw = CodeBlock.builder();
            sw.add("return switch (propertyName) {\n").indent();
            for (FieldInfo f : links) sw.add("case $S -> new $T();\n", f.name, getRawType(f.linkTargetType));
            sw.add("default -> null;\n").unindent().add("};");
            builder.addCode(sw.build());
        }
        return builder.build();
    }

    // ===== Enum field support (PRP-28 phase 1, PRP-30) — class targets only (_A on either path) =====

    /**
     * For each enum-valued field — a bare {@code E} or a {@code List<E>} — add a private static final
     * {@code Map<String, E>} built once from {@code E.values()} (never {@code getEnumConstants()} —
     * that would be reflection), keyed by {@code uuid()} for an {@code @AsUuid} enum or {@code name()}
     * for {@code @AsName}. Then override {@code isEnumField}/{@code isEnumListField}/{@code
     * resolveEnumFromStorage} to dispatch to those maps.
     *
     * <p>The two shapes share one resolver: a list element and a bare field are the same stored
     * string, so {@code resolveEnumFromStorage} is keyed by field name alone and the caller decides
     * whether it is resolving one value or each element of a list.</p>
     *
     * <p>This is the read-side counterpart to {@link xyz.jphil.datahelper.HasUuid#storageValue(Object)}
     * on write: write is a generic runtime {@code instanceof} check (the concrete value is in hand),
     * but read only has a {@code Class<?>} handle and a stored String, so the concrete map has to be
     * generated per field, per entity, at the one place the concrete enum type is statically known.
     * A lookup miss returns {@code null} — never throws — because an unmatched id means the row was
     * written by newer code, not that it is corrupt.
     *
     * <p>Emitted on whichever generated type is the one a value is read INTO: the {@code _A} sealed
     * base on the {@code @Data} and {@code @ArcadeData} paths, and the {@code _I} writable interface
     * on the {@code @DataHelper} path, which has no class of its own. An interface target only
     * changes modifiers — its fields are implicitly {@code public static final} — so both paths get
     * the same lookup. Every path needs it: an embedded block is a {@code @Data} type, and a
     * {@code @DataHelper} DTO reads back through {@code fromMap}/{@code fromJson} like any other.
     */
    public static void addEnumSupport(TypeSpec.Builder builder, List<FieldInfo> fields, boolean isInterface) {
        List<FieldInfo> enumFields = fields.stream().filter(FieldInfo::isAnyEnum).toList();
        if (enumFields.isEmpty()) return;

        ClassName mapClass = ClassName.get(Map.class);
        ClassName hashMapClass = ClassName.get(HashMap.class);
        ClassName stringClass = ClassName.get(String.class);
        Modifier[] fieldModifiers = isInterface
                ? new Modifier[]{Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL}
                : new Modifier[]{Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL};

        for (FieldInfo f : enumFields) {
            TypeName enumType = getRawType(f.enumType);
            String cap = ProcessorUtils.capitalize(f.name);
            String builderMethodName = "$build" + cap + "EnumMap";
            String mapFieldName = "$" + f.name + "$ENUM_MAP";
            String keyAccessor = f.isEnumAsUuid ? "uuid" : "name";
            ParameterizedTypeName mapType = ParameterizedTypeName.get(mapClass, stringClass, enumType);

            builder.addMethod(MethodSpec.methodBuilder(builderMethodName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .returns(mapType)
                    .addStatement("var m = new $T<$T, $T>()", hashMapClass, stringClass, enumType)
                    .addStatement("for ($T c : $T.values()) m.put(c.$N(), c)", enumType, enumType, keyAccessor)
                    .addStatement("return m")
                    .build());

            builder.addField(FieldSpec.builder(mapType, mapFieldName, fieldModifiers)
                    .initializer("$N()", builderMethodName)
                    .build());
        }

        builder.addMethod(booleanFieldPredicate("isEnumField", fields, f -> f.isEnum, isInterface));
        builder.addMethod(booleanFieldPredicate("isEnumListField", fields, f -> f.isEnumList, isInterface));

        MethodSpec.Builder resolve = MethodSpec.methodBuilder("resolveEnumFromStorage")
                .addModifiers(implModifiers(isInterface))
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .addParameter(String.class, "storedValue")
                .returns(Object.class);
        CodeBlock.Builder sw = CodeBlock.builder();
        sw.add("return switch (propertyName) {\n").indent();
        for (FieldInfo f : enumFields) {
            sw.add("case $S -> $N.get(storedValue);\n", f.name, "$" + f.name + "$ENUM_MAP");
        }
        sw.add("default -> null;\n").unindent().add("};");
        resolve.addCode(sw.build());
        builder.addMethod(resolve.build());
    }
}
