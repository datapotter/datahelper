package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Analyzes and validates fields in classes annotated for processing.
 * Extracts field information and validates field types (List, Map, DataHelper, etc.).
 */
public class FieldAnalyzer {

    private final ProcessingEnvironment processingEnv;
    private final ProcessorUtils utils;

    public FieldAnalyzer(ProcessingEnvironment processingEnv, ProcessorUtils utils) {
        this.processingEnv = processingEnv;
        this.utils = utils;
    }

    /**
     * Extract and validate all fields from a class.
     * Returns null if validation errors were found.
     */
    public List<FieldInfo> analyzeFields(TypeElement element) {
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
                    boolean isListField = utils.isListType(fieldTypeMirror);
                    boolean isMapField = utils.isMapType(fieldTypeMirror);
                    boolean isNestedDataHelper = !isListField && !isMapField && utils.isDataHelperType(fieldTypeMirror);

                    TypeName listElementType = null;
                    boolean isListOfDataHelper = false;
                    String listImplClass = null;

                    TypeName mapKeyType = null;
                    TypeName mapValueType = null;
                    boolean isMapOfDataHelper = false;
                    String mapImplClass = null;

                    // === List Field Analysis and Validation ===
                    if (isListField) {
                        hasErrors |= analyzeListField(field, fieldName, fieldTypeMirror);

                        DeclaredType declaredType = (DeclaredType) fieldTypeMirror;
                        if (!declaredType.getTypeArguments().isEmpty()) {
                            listElementType = utils.getListElementType(fieldTypeMirror);
                            TypeMirror elementTypeMirror = utils.getListElementTypeMirror(fieldTypeMirror);
                            isListOfDataHelper = utils.isDataHelperType(elementTypeMirror);
                            listImplClass = utils.getListImplementationClass(fieldTypeMirror);
                        }
                    }
                    // === Map Field Analysis and Validation ===
                    else if (isMapField) {
                        hasErrors |= analyzeMapField(field, fieldName, fieldTypeMirror);

                        DeclaredType declaredType = (DeclaredType) fieldTypeMirror;
                        if (declaredType.getTypeArguments().size() >= 2) {
                            mapKeyType = utils.getMapKeyType(fieldTypeMirror);
                            mapValueType = utils.getMapValueType(fieldTypeMirror);
                            TypeMirror valueTypeMirror = utils.getMapValueTypeMirror(fieldTypeMirror);
                            isMapOfDataHelper = utils.isDataHelperType(valueTypeMirror);
                            mapImplClass = utils.getMapImplementationClass(fieldTypeMirror);
                        }
                    }
                    // === Simple Field Validation ===
                    else if (!isListField && !isMapField && !isNestedDataHelper && !utils.isSupportedSimpleType(fieldTypeMirror)) {
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

        return hasErrors ? null : fields;
    }

    /**
     * Analyze and validate a List field.
     * Returns true if errors were found.
     */
    private boolean analyzeListField(VariableElement field, String fieldName, TypeMirror fieldTypeMirror) {
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
            return true;
        }

        TypeMirror elementTypeMirror = utils.getListElementTypeMirror(fieldTypeMirror);

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
            return true;
        }

        // Validate: Nested collections (List<List<T>>, List<Map<K,V>>)
        if (utils.isListType(elementTypeMirror) || utils.isMapType(elementTypeMirror)) {
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
            return true;
        }

        // Validate element type
        boolean isListOfDataHelper = utils.isDataHelperType(elementTypeMirror);
        if (!isListOfDataHelper && !utils.isSupportedSimpleType(elementTypeMirror)) {
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
            return true;
        }

        return false;
    }

    /**
     * Analyze and validate a Map field.
     * Returns true if errors were found.
     */
    private boolean analyzeMapField(VariableElement field, String fieldName, TypeMirror fieldTypeMirror) {
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
            return true;
        }

        TypeMirror keyTypeMirror = utils.getMapKeyTypeMirror(fieldTypeMirror);
        TypeMirror valueTypeMirror = utils.getMapValueTypeMirror(fieldTypeMirror);

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
            return true;
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
            return true;
        }

        // Validate: Map keys must be simple types (not DataHelper)
        if (utils.isDataHelperType(keyTypeMirror)) {
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
            return true;
        }

        // Validate: Nested collections in Map
        if (utils.isListType(valueTypeMirror) || utils.isMapType(valueTypeMirror)) {
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
            return true;
        }

        // Validate key type (must be simple)
        if (!utils.isSupportedSimpleType(keyTypeMirror)) {
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
            return true;
        }

        // Validate value type
        boolean isMapOfDataHelper = utils.isDataHelperType(valueTypeMirror);
        if (!isMapOfDataHelper && !utils.isSupportedSimpleType(valueTypeMirror)) {
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
            return true;
        }

        return false;
    }
}
