package xyz.jphil.datahelper;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Annotation processor that generates {ClassName}_I interface with property accessors.
 *
 * Generated code includes:
 * - Field symbols ($fieldName constants)
 * - Fluent getters and setters
 * - 14 property accessor methods for DataHelper_I (8 for List/Object, 6 for Map)
 * - FIELDS list (immutable)
 *
 * <p>The generated interface extends DataHelper_I and implements all property accessor methods.
 * Serialization traits (JSON, ArcadeDB, etc.) use these property accessors to convert data.</p>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("xyz.jphil.datahelper.DataHelper")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DataHelperProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "Processing @DataHelper on " + element);
                    generateInterface((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void generateInterface(TypeElement element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).toString();
        String className = element.getSimpleName().toString();
        String interfaceName = className + "_I";

        // Collect all non-static, non-final fields
        List<FieldInfo> fields = new ArrayList<>();
        boolean hasErrors = false;

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                Set<Modifier> modifiers = enclosedElement.getModifiers();
                if (!modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.FINAL)) {
                    VariableElement field = (VariableElement) enclosedElement;
                    String fieldName = field.getSimpleName().toString();
                    TypeMirror fieldTypeMirror = field.asType();
                    TypeName fieldType = TypeName.get(fieldTypeMirror);

                    // Analyze field type
                    boolean isListField = isListType(fieldTypeMirror);
                    boolean isMapField = isMapType(fieldTypeMirror);
                    boolean isNestedDataHelper = !isListField && !isMapField && isDataHelperType(fieldTypeMirror);

                    TypeName listElementType = null;
                    boolean isListOfDataHelper = false;
                    String listImplClass = null;

                    TypeName mapKeyType = null;
                    TypeName mapValueType = null;
                    boolean isMapOfDataHelper = false;
                    String mapImplClass = null;

                    // === List Field Analysis and Validation ===
                    if (isListField) {
                        DeclaredType declaredType = (DeclaredType) fieldTypeMirror;

                        // Validate: Raw List (no type arguments)
                        if (declaredType.getTypeArguments().isEmpty()) {
                            processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format(
                                    "Field '%s' uses raw List without type parameter. " +
                                    "Use List<T> with explicit element type (e.g., List<String>, List<Integer>, List<PersonDTO>).",
                                    fieldName
                                ),
                                field
                            );
                            hasErrors = true;
                        } else {
                            listElementType = getListElementType(fieldTypeMirror);
                            TypeMirror elementTypeMirror = getListElementTypeMirror(fieldTypeMirror);

                            // Validate: List<Object>
                            if (elementTypeMirror.toString().equals("java.lang.Object")) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses List<Object> which is not supported. " +
                                        "Use explicit type (List<String>, List<Integer>, etc.). " +
                                        "For dynamic structures, use a serialization trait that supports untyped data.",
                                        fieldName
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }
                            // Validate: Nested collections (List<List<T>>, List<Map<K,V>>)
                            else if (isListType(elementTypeMirror) || isMapType(elementTypeMirror)) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses nested collection %s which is not supported. " +
                                        "Use a wrapper class with @DataHelper instead:\n\n" +
                                        "@DataHelper\n" +
                                        "class Wrapper { %s items; }\n\n" +
                                        "Then use: List<Wrapper> %s;",
                                        fieldName,
                                        fieldTypeMirror,
                                        elementTypeMirror,
                                        fieldName
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }
                            // Validate element type
                            else {
                                isListOfDataHelper = isDataHelperType(elementTypeMirror);
                                if (!isListOfDataHelper && !isSupportedSimpleType(elementTypeMirror)) {
                                    processingEnv.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        String.format(
                                            "Field '%s' is a List with unsupported element type '%s'. " +
                                            "List elements must be: primitives, boxed primitives, String, or @DataHelper types.",
                                            fieldName,
                                            elementTypeMirror
                                        ),
                                        field
                                    );
                                    hasErrors = true;
                                }
                            }

                            listImplClass = getListImplementationClass(fieldTypeMirror);
                        }
                    }

                    // === Map Field Analysis and Validation ===
                    else if (isMapField) {
                        DeclaredType declaredType = (DeclaredType) fieldTypeMirror;

                        // Validate: Raw Map (no type arguments)
                        if (declaredType.getTypeArguments().size() < 2) {
                            processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format(
                                    "Field '%s' uses raw Map without type parameters. " +
                                    "Use Map<K,V> with explicit key and value types (e.g., Map<String, Integer>, Map<String, PersonDTO>).",
                                    fieldName
                                ),
                                field
                            );
                            hasErrors = true;
                        } else {
                            mapKeyType = getMapKeyType(fieldTypeMirror);
                            mapValueType = getMapValueType(fieldTypeMirror);
                            TypeMirror keyTypeMirror = getMapKeyTypeMirror(fieldTypeMirror);
                            TypeMirror valueTypeMirror = getMapValueTypeMirror(fieldTypeMirror);

                            // Validate: Map<Object, V> or Map<K, Object>
                            if (keyTypeMirror.toString().equals("java.lang.Object")) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses Map<Object, ...> which is not supported. " +
                                        "Map keys must be explicit simple types (String, Integer, etc.). " +
                                        "For dynamic structures, use a serialization trait that supports untyped data.",
                                        fieldName
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }
                            if (valueTypeMirror.toString().equals("java.lang.Object")) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses Map<..., Object> which is not supported. " +
                                        "Map values must be explicit types (String, Integer, PersonDTO, etc.). " +
                                        "For dynamic structures, use a serialization trait that supports untyped data.",
                                        fieldName
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }

                            // Validate: Map keys must be simple types (not DataHelper)
                            if (isDataHelperType(keyTypeMirror)) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses Map<%s, ...> with DataHelper type as key. " +
                                        "Map keys must be simple types (String, Integer, etc.) for serialization compatibility. " +
                                        "Use Map<String, %s> instead, or use object.id as key.",
                                        fieldName,
                                        keyTypeMirror,
                                        valueTypeMirror
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }

                            // Validate: Nested collections in Map
                            if (isListType(valueTypeMirror) || isMapType(valueTypeMirror)) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' uses nested collection %s which is not supported. " +
                                        "Use a wrapper class with @DataHelper instead:\n\n" +
                                        "@DataHelper\n" +
                                        "class Wrapper { %s data; }\n\n" +
                                        "Then use: Map<%s, Wrapper> %s;",
                                        fieldName,
                                        fieldTypeMirror,
                                        valueTypeMirror,
                                        keyTypeMirror,
                                        fieldName
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }

                            // Validate key type (must be simple)
                            if (!isSupportedSimpleType(keyTypeMirror)) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' has Map with unsupported key type '%s'. " +
                                        "Map keys must be: primitives, boxed primitives, or String.",
                                        fieldName,
                                        keyTypeMirror
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }

                            // Validate value type
                            isMapOfDataHelper = isDataHelperType(valueTypeMirror);
                            if (!isMapOfDataHelper && !isSupportedSimpleType(valueTypeMirror)) {
                                processingEnv.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    String.format(
                                        "Field '%s' has Map with unsupported value type '%s'. " +
                                        "Map values must be: primitives, boxed primitives, String, or @DataHelper types.",
                                        fieldName,
                                        valueTypeMirror
                                    ),
                                    field
                                );
                                hasErrors = true;
                            }

                            mapImplClass = getMapImplementationClass(fieldTypeMirror);
                        }
                    }

                    // === Simple Field Validation ===
                    else if (!isListField && !isMapField && !isNestedDataHelper && !isSupportedSimpleType(fieldTypeMirror)) {
                        processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format(
                                "Field '%s' has unsupported type '%s'. " +
                                "Supported types are: primitives, boxed primitives, String, @DataHelper annotated types, " +
                                "types implementing DataHelper_I, List<T>, and Map<K,V> of these types.",
                                fieldName,
                                fieldTypeMirror
                            ),
                            field
                        );
                        hasErrors = true;
                    }

                    fields.add(new FieldInfo(fieldName, fieldType, isListField, isNestedDataHelper,
                                             isListOfDataHelper, listElementType, isMapField,
                                             mapKeyType, mapValueType, isMapOfDataHelper,
                                             listImplClass, mapImplClass));
                }
            }
        }

        // If there were validation errors, stop processing
        if (hasErrors) {
            return;
        }

        // Build the interface
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("E",
                        ParameterizedTypeName.get(
                                ClassName.get(packageName, interfaceName),
                                TypeVariableName.get("E")
                        )));

        // Always extend DataHelper_I<E> as the base contract
        interfaceBuilder.addSuperinterface(ParameterizedTypeName.get(
                ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                TypeVariableName.get("E")
        ));

        // Add field symbols
        for (FieldInfo field : fields) {
            String symbolName = "$" + field.name;
            FieldSpec symbol = FieldSpec.builder(String.class, symbolName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", field.name)
                    .build();
            interfaceBuilder.addField(symbol);
        }

        // Add immutable FIELDS list
        CodeBlock.Builder fieldsListInitBuilder = CodeBlock.builder().add("$T.of(", List.class);
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                fieldsListInitBuilder.add(", ");
            }
            fieldsListInitBuilder.add("$N", "$" + fields.get(i).name);
        }
        fieldsListInitBuilder.add(")");

        FieldSpec fieldsListField = FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)),
                "FIELDS",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(fieldsListInitBuilder.build())
                .build();
        interfaceBuilder.addField(fieldsListField);

        // Add verbose getter declarations
        for (FieldInfo field : fields) {
            String getterName = "get" + capitalize(field.name);
            MethodSpec getter = MethodSpec.methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(field.type)
                    .build();
            interfaceBuilder.addMethod(getter);
        }

        // Add verbose setter declarations
        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            MethodSpec setter = MethodSpec.methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(field.type, field.name)
                    .build();
            interfaceBuilder.addMethod(setter);
        }

        // Add fluent getter defaults
        for (FieldInfo field : fields) {
            String getterName = "get" + capitalize(field.name);
            MethodSpec fluentGetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(field.type)
                    .addStatement("return $N()", getterName)
                    .build();
            interfaceBuilder.addMethod(fluentGetter);
        }

        // Add fluent setter defaults
        for (FieldInfo field : fields) {
            String setterName = "set" + capitalize(field.name);
            MethodSpec fluentSetter = MethodSpec.methodBuilder(field.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .addParameter(field.type, field.name)
                    .returns(TypeVariableName.get("E"))
                    .addStatement("$N($N)", setterName, field.name)
                    .addStatement("return (E) this")
                    .build();
            interfaceBuilder.addMethod(fluentSetter);
        }

        // ========== Property Accessor Methods (15 methods for delegation) ==========
        // Method 0: dataClass() - Returns concrete class object
        // Methods 1-8: Core property accessors (List/Object support)
        // Methods 9-14: Map support

        // 0. dataClass() - New method for type recovery
        MethodSpec dataClassMethod = MethodSpec.methodBuilder("dataClass")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)))
                .addStatement("return $T.class", ClassName.get(packageName, className))
                .build();
        interfaceBuilder.addMethod(dataClassMethod);

        // 1. fieldNames()
        MethodSpec fieldNamesMethod = MethodSpec.methodBuilder("fieldNames")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)))
                .addStatement("return FIELDS")
                .build();
        interfaceBuilder.addMethod(fieldNamesMethod);

        // 2. getPropertyByName(String)
        MethodSpec.Builder getPropertyByNameBuilder = MethodSpec.methodBuilder("getPropertyByName")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(Object.class);

        if (fields.isEmpty()) {
            getPropertyByNameBuilder.addStatement("return null");
        } else {
            // Build switch expression as a statement (with semicolon)
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : fields) {
                String getterName = "get" + capitalize(field.name);
                switchBlock.add("case $N -> $N();\n", "$" + field.name, getterName);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            getPropertyByNameBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(getPropertyByNameBuilder.build());

        // 3. setPropertyByName(String, Object)
        MethodSpec.Builder setPropertyByNameBuilder = MethodSpec.methodBuilder("setPropertyByName")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addParameter(String.class, "propertyName")
                .addParameter(Object.class, "value");

        if (!fields.isEmpty()) {
            setPropertyByNameBuilder.beginControlFlow("switch (propertyName)");
            for (FieldInfo field : fields) {
                String setterName = "set" + capitalize(field.name);
                // Use convertType for primitives and wrapper types to handle boxing/unboxing
                if (field.type.isPrimitive() || isWrapperType(field.type)) {
                    // Get the wrapper class for primitives
                    TypeName wrapperType = field.type.isPrimitive() ? field.type.box() : field.type;
                    setPropertyByNameBuilder.addStatement("case $N -> $N(($T) $T.convertType(value, $T.class))",
                            "$" + field.name, setterName, field.type,
                            ClassName.get("xyz.jphil.datahelper", "DataHelper_I"), wrapperType);
                } else {
                    setPropertyByNameBuilder.addStatement("case $N -> $N(($T) value)", "$" + field.name, setterName, field.type);
                }
            }
            setPropertyByNameBuilder.endControlFlow();
        }
        interfaceBuilder.addMethod(setPropertyByNameBuilder.build());

        // 4. getPropertyType(String)
        MethodSpec.Builder getPropertyTypeBuilder = MethodSpec.methodBuilder("getPropertyType")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        if (fields.isEmpty()) {
            getPropertyTypeBuilder.addStatement("return null");
        } else {
            // Build switch expression as a statement (with semicolon)
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : fields) {
                // For parameterized types, use raw type
                if (field.type instanceof ParameterizedTypeName) {
                    ParameterizedTypeName paramType = (ParameterizedTypeName) field.type;
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, paramType.rawType);
                } else {
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, field.type);
                }
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            getPropertyTypeBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(getPropertyTypeBuilder.build());

        // 5. createNestedObject(String)
        MethodSpec.Builder createNestedObjectBuilder = MethodSpec.methodBuilder("createNestedObject")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> nestedFields = fields.stream().filter(f -> f.isNestedDataHelper).toList();
        if (nestedFields.isEmpty()) {
            createNestedObjectBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : nestedFields) {
                switchBlock.add("case $N -> new $T();\n", "$" + field.name, field.type);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            createNestedObjectBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(createNestedObjectBuilder.build());

        // 6. createListElement(String)
        MethodSpec.Builder createListElementBuilder = MethodSpec.methodBuilder("createListElement")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        List<FieldInfo> listOfDataHelperFields = fields.stream().filter(f -> f.isListOfDataHelper).toList();
        if (listOfDataHelperFields.isEmpty()) {
            createListElementBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : listOfDataHelperFields) {
                switchBlock.add("case $N -> new $T();\n", "$" + field.name, field.listElementType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            createListElementBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(createListElementBuilder.build());

        // 7. isListField(String)
        MethodSpec.Builder isListFieldBuilder = MethodSpec.methodBuilder("isListField")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> listFields = fields.stream().filter(f -> f.isListField).toList();
        if (listFields.isEmpty()) {
            isListFieldBuilder.addStatement("return false");
        } else if (listFields.size() == 1) {
            isListFieldBuilder.addStatement("return $N.equals(propertyName)", "$" + listFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : listFields) {
                switchBlock.add("case $N -> true;\n", "$" + field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            isListFieldBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(isListFieldBuilder.build());

        // 8. isNestedObjectField(String)
        MethodSpec.Builder isNestedObjectFieldBuilder = MethodSpec.methodBuilder("isNestedObjectField")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        if (nestedFields.isEmpty()) {
            isNestedObjectFieldBuilder.addStatement("return false");
        } else if (nestedFields.size() == 1) {
            isNestedObjectFieldBuilder.addStatement("return $N.equals(propertyName)", "$" + nestedFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : nestedFields) {
                switchBlock.add("case $N -> true;\n", "$" + field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            isNestedObjectFieldBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(isNestedObjectFieldBuilder.build());

        // 9. isMapField(String)
        MethodSpec.Builder isMapFieldBuilder = MethodSpec.methodBuilder("isMapField")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> mapFields = fields.stream().filter(f -> f.isMapField).toList();
        if (mapFields.isEmpty()) {
            isMapFieldBuilder.addStatement("return false");
        } else if (mapFields.size() == 1) {
            isMapFieldBuilder.addStatement("return $N.equals(propertyName)", "$" + mapFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                switchBlock.add("case $N -> true;\n", "$" + field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            isMapFieldBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(isMapFieldBuilder.build());

        // 10. getMapKeyType(String)
        MethodSpec.Builder getMapKeyTypeBuilder = MethodSpec.methodBuilder("getMapKeyType")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        if (mapFields.isEmpty()) {
            getMapKeyTypeBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                // For parameterized types, use raw type
                if (field.mapKeyType instanceof ParameterizedTypeName) {
                    ParameterizedTypeName paramType = (ParameterizedTypeName) field.mapKeyType;
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, paramType.rawType);
                } else {
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, field.mapKeyType);
                }
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            getMapKeyTypeBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(getMapKeyTypeBuilder.build());

        // 11. getMapValueType(String)
        MethodSpec.Builder getMapValueTypeBuilder = MethodSpec.methodBuilder("getMapValueType")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)));

        if (mapFields.isEmpty()) {
            getMapValueTypeBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                // For parameterized types, use raw type
                if (field.mapValueType instanceof ParameterizedTypeName) {
                    ParameterizedTypeName paramType = (ParameterizedTypeName) field.mapValueType;
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, paramType.rawType);
                } else {
                    switchBlock.add("case $N -> $T.class;\n", "$" + field.name, field.mapValueType);
                }
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            getMapValueTypeBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(getMapValueTypeBuilder.build());

        // 12. createMapInstance(String)
        MethodSpec.Builder createMapInstanceBuilder = MethodSpec.methodBuilder("createMapInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        WildcardTypeName.subtypeOf(Object.class),
                        WildcardTypeName.subtypeOf(Object.class)));

        if (mapFields.isEmpty()) {
            createMapInstanceBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapFields) {
                // Use the concrete implementation class
                String implClass = field.mapImplClass != null ? field.mapImplClass : "java.util.LinkedHashMap";
                switchBlock.add("case $N -> new $L<>();\n", "$" + field.name, implClass);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            createMapInstanceBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(createMapInstanceBuilder.build());

        // 13. isMapValueDataHelper(String)
        MethodSpec.Builder isMapValueDataHelperBuilder = MethodSpec.methodBuilder("isMapValueDataHelper")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(boolean.class);

        List<FieldInfo> mapOfDataHelperFields = fields.stream().filter(f -> f.isMapOfDataHelper).toList();
        if (mapOfDataHelperFields.isEmpty()) {
            isMapValueDataHelperBuilder.addStatement("return false");
        } else if (mapOfDataHelperFields.size() == 1) {
            isMapValueDataHelperBuilder.addStatement("return $N.equals(propertyName)", "$" + mapOfDataHelperFields.get(0).name);
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapOfDataHelperFields) {
                switchBlock.add("case $N -> true;\n", "$" + field.name);
            }
            switchBlock.add("default -> false;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            isMapValueDataHelperBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(isMapValueDataHelperBuilder.build());

        // 14. createMapValueElement(String)
        MethodSpec.Builder createMapValueElementBuilder = MethodSpec.methodBuilder("createMapValueElement")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .addParameter(String.class, "propertyName")
                .returns(ParameterizedTypeName.get(
                        ClassName.get("xyz.jphil.datahelper", "DataHelper_I"),
                        WildcardTypeName.subtypeOf(Object.class)));

        if (mapOfDataHelperFields.isEmpty()) {
            createMapValueElementBuilder.addStatement("return null");
        } else {
            CodeBlock.Builder switchBlock = CodeBlock.builder();
            switchBlock.add("return switch (propertyName) {\n");
            switchBlock.indent();
            for (FieldInfo field : mapOfDataHelperFields) {
                switchBlock.add("case $N -> new $T();\n", "$" + field.name, field.mapValueType);
            }
            switchBlock.add("default -> null;\n");
            switchBlock.unindent();
            switchBlock.add("};");  // Semicolon AFTER closing brace!
            createMapValueElementBuilder.addCode(switchBlock.build());
        }
        interfaceBuilder.addMethod(createMapValueElementBuilder.build());

        // Build and write the file
        TypeSpec interfaceSpec = interfaceBuilder.build();
        JavaFile javaFile = JavaFile.builder(packageName, interfaceSpec)
                .indent("    ")
                .skipJavaLangImports(true)
                .addFileComment("Generated by DataHelperProcessor on " + LocalDateTime.now())
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Generated interface: " + interfaceName + " for class: " + className);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate interface: " + e.getMessage());
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Check if type is a wrapper type (Integer, Long, Double, etc.)
     */
    private boolean isWrapperType(TypeName type) {
        return type.equals(TypeName.get(Integer.class)) ||
               type.equals(TypeName.get(Long.class)) ||
               type.equals(TypeName.get(Double.class)) ||
               type.equals(TypeName.get(Float.class)) ||
               type.equals(TypeName.get(Short.class)) ||
               type.equals(TypeName.get(Byte.class)) ||
               type.equals(TypeName.get(Boolean.class)) ||
               type.equals(TypeName.get(Character.class));
    }

    /**
     * Check if type is List<T> or a supported List implementation
     */
    private boolean isListType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;

        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        return qualifiedName.equals("java.util.List") ||
               qualifiedName.equals("java.util.ArrayList") ||
               qualifiedName.equals("java.util.LinkedList");
    }

    /**
     * Check if type is Map<K,V> or a supported Map implementation
     */
    private boolean isMapType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;

        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        return qualifiedName.equals("java.util.Map") ||
               qualifiedName.equals("java.util.HashMap") ||
               qualifiedName.equals("java.util.LinkedHashMap");
    }

    /**
     * Get concrete List implementation class name (for instantiation)
     */
    private String getListImplementationClass(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return "java.util.ArrayList";

        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        if (qualifiedName.equals("java.util.ArrayList")) return "java.util.ArrayList";
        if (qualifiedName.equals("java.util.LinkedList")) return "java.util.LinkedList";
        return "java.util.ArrayList"; // Default for java.util.List
    }

    /**
     * Get concrete Map implementation class name (for instantiation)
     */
    private String getMapImplementationClass(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return "java.util.LinkedHashMap";

        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();

        if (qualifiedName.equals("java.util.HashMap")) return "java.util.HashMap";
        if (qualifiedName.equals("java.util.LinkedHashMap")) return "java.util.LinkedHashMap";
        return "java.util.LinkedHashMap"; // Default for java.util.Map (preserves order)
    }

    /**
     * Check if type is a supported simple type (primitive, boxed, or String).
     * These types can be directly serialized/deserialized without custom handling.
     */
    private boolean isSupportedSimpleType(TypeMirror type) {
        // Primitives
        if (type.getKind().isPrimitive()) {
            return true;
        }

        // String and boxed primitives
        String typeName = type.toString();
        return typeName.equals("java.lang.String") ||
               typeName.equals("java.lang.Integer") ||
               typeName.equals("java.lang.Long") ||
               typeName.equals("java.lang.Double") ||
               typeName.equals("java.lang.Float") ||
               typeName.equals("java.lang.Boolean") ||
               typeName.equals("java.lang.Short") ||
               typeName.equals("java.lang.Byte") ||
               typeName.equals("java.lang.Character");
    }

    /**
     * Check if type is a DataHelper type (can be recursively processed).
     *
     * Three cases:
     * 1. Has @DataHelper annotation (will generate DataHelper_I implementation)
     *    - Checked first to handle first compile pass (chicken-and-egg problem)
     * 2. Already implements DataHelper_I (hand-written or from previous compile)
     *    - Allows custom implementations without annotation
     * 3. Neither - treated as opaque type (no recursive processing)
     */
    private boolean isDataHelperType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;

        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        if (typeElement == null) return false;

        // Case 1: Check if it's annotated with @DataHelper
        // (This handles first compile pass - annotation processor generated types)
        if (typeElement.getAnnotation(DataHelper.class) != null) {
            return true;
        }

        // Case 2: Check if it implements DataHelper_I
        // (This handles hand-written implementations and already-compiled types)
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (interfaceType.toString().startsWith("xyz.jphil.datahelper.DataHelper_I")) {
                return true;
            }
        }

        // Case 3: Neither - not a DataHelper type
        // Will be treated as opaque (simple property, no recursive processing)
        return false;
    }

    /**
     * Get element type from List<T>
     */
    private TypeName getListElementType(TypeMirror type) {
        if (!isListType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().isEmpty()) return null;

        return TypeName.get(declaredType.getTypeArguments().get(0));
    }

    /**
     * Get element TypeMirror from List<T>
     */
    private TypeMirror getListElementTypeMirror(TypeMirror type) {
        if (!isListType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().isEmpty()) return null;

        return declaredType.getTypeArguments().get(0);
    }

    /**
     * Get key TypeName from Map<K,V>
     */
    private TypeName getMapKeyType(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return TypeName.get(declaredType.getTypeArguments().get(0));
    }

    /**
     * Get value TypeName from Map<K,V>
     */
    private TypeName getMapValueType(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return TypeName.get(declaredType.getTypeArguments().get(1));
    }

    /**
     * Get key TypeMirror from Map<K,V>
     */
    private TypeMirror getMapKeyTypeMirror(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return declaredType.getTypeArguments().get(0);
    }

    /**
     * Get value TypeMirror from Map<K,V>
     */
    private TypeMirror getMapValueTypeMirror(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return declaredType.getTypeArguments().get(1);
    }

    private static class FieldInfo {
        final String name;
        final TypeName type;
        final boolean isListField;
        final boolean isNestedDataHelper;
        final boolean isListOfDataHelper;
        final TypeName listElementType;
        final boolean isMapField;
        final TypeName mapKeyType;
        final TypeName mapValueType;
        final boolean isMapOfDataHelper;
        final String listImplClass;
        final String mapImplClass;

        FieldInfo(String name, TypeName type, boolean isListField, boolean isNestedDataHelper,
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
}
