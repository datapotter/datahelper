package datapotter.datahelper.processor.util;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import datapotter.datahelper.AsName;
import datapotter.datahelper.AsUuid;
import datapotter.datahelper.DataHelper;
import datapotter.datahelper.EnumData;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

/**
 * Shared utility methods for annotation processors.
 * Contains type detection, validation, and helper methods used across processors.
 */
public class ProcessorUtils {

    /**
     * Default set of annotation qualified names that mark a type as a DataHelper.
     * Subclasses/extensions can add their own annotations (e.g., @ArcadeData).
     *
     * <p>This allows the ArcadeDB processor to extend this list with @ArcadeData,
     * and future annotations are automatically inherited.
     */
    public static final Set<String> BASE_DATA_HELPER_ANNOTATIONS = Set.of(
        "datapotter.datahelper.DataHelper",
        "datapotter.datahelper.Data"
    );

    private final ProcessingEnvironment processingEnv;
    private final Set<String> dataHelperAnnotations;

    /**
     * Constructor with default annotations (DataHelper, Data).
     */
    public ProcessorUtils(ProcessingEnvironment processingEnv) {
        this(processingEnv, BASE_DATA_HELPER_ANNOTATIONS);
    }

    /**
     * Constructor with custom annotation set.
     * Allows extending the default annotations (e.g., for ArcadeDB to add @ArcadeData).
     *
     * @param processingEnv the processing environment
     * @param dataHelperAnnotations set of fully qualified annotation names
     */
    public ProcessorUtils(ProcessingEnvironment processingEnv, Set<String> dataHelperAnnotations) {
        this.processingEnv = processingEnv;
        this.dataHelperAnnotations = dataHelperAnnotations;
    }

    /**
     * Capitalize first letter of a string.
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Check if type is boolean (primitive or wrapper).
     * For boolean types, we should use "is" prefix for getters.
     */
    public static boolean isBooleanType(TypeName type) {
        return type.equals(TypeName.BOOLEAN) ||
               type.equals(TypeName.get(Boolean.class));
    }

    /**
     * Check if type is a wrapper type (Integer, Long, Double, etc.)
     */
    public static boolean isWrapperType(TypeName type) {
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
    public boolean isListType(TypeMirror type) {
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
    public boolean isMapType(TypeMirror type) {
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
    public String getListImplementationClass(TypeMirror type) {
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
    public String getMapImplementationClass(TypeMirror type) {
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
    public boolean isSupportedSimpleType(TypeMirror type) {
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
     * 1. Has any configured DataHelper annotation (@DataHelper, @Data, @ArcadeData, etc.)
     * 2. Already implements DataHelper_I (hand-written or from previous compile)
     * 3. Neither - treated as opaque type (no recursive processing)
     */
    public boolean isDataHelperType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;

        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        if (typeElement == null) return false;

        // Case 1: Check if it's annotated with any configured DataHelper annotation
        for (String annotationName : dataHelperAnnotations) {
            // Check by annotation qualified name
            if (typeElement.getAnnotationMirrors().stream()
                    .anyMatch(mirror -> mirror.getAnnotationType().toString().equals(annotationName))) {
                return true;
            }
        }

        // Case 2: Check if it implements DataHelper_I
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (interfaceType.toString().startsWith("datapotter.datahelper.DataHelper_I")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a type is an <em>annotation-generated</em> DataHelper, i.e. it carries one of the
     * configured DataHelper annotations ({@code @DataHelper}/{@code @Data}/{@code @ArcadeData}/&hellip;).
     *
     * <p>Only such types get generated {@code _IR}/{@code _I}/{@code _R} siblings, so this is the
     * predicate that decides whether a nested component can participate in getter widening and
     * record projection. A hand-written DataHelper (case 2 of {@link #isDataHelperType}) returns
     * {@code false} here and is treated as an opaque concrete type with identity conversion.</p>
     */
    public boolean isGeneratedDataHelperType(TypeMirror type) {
        if (type == null || type.getKind() != TypeKind.DECLARED) return false;

        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(type);
        if (typeElement == null) return false;

        for (String annotationName : dataHelperAnnotations) {
            if (typeElement.getAnnotationMirrors().stream()
                    .anyMatch(mirror -> mirror.getAnnotationType().toString().equals(annotationName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get element type from List<T>
     */
    public TypeName getListElementType(TypeMirror type) {
        if (!isListType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().isEmpty()) return null;

        return TypeName.get(declaredType.getTypeArguments().get(0));
    }

    /**
     * Get element TypeMirror from List<T>
     */
    public TypeMirror getListElementTypeMirror(TypeMirror type) {
        if (!isListType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().isEmpty()) return null;

        return declaredType.getTypeArguments().get(0);
    }

    /**
     * Get key TypeName from Map<K,V>
     */
    public TypeName getMapKeyType(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return TypeName.get(declaredType.getTypeArguments().get(0));
    }

    /**
     * Get value TypeName from Map<K,V>
     */
    public TypeName getMapValueType(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return TypeName.get(declaredType.getTypeArguments().get(1));
    }

    /**
     * Get key TypeMirror from Map<K,V>
     */
    public TypeMirror getMapKeyTypeMirror(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return declaredType.getTypeArguments().get(0);
    }

    /**
     * Get value TypeMirror from Map<K,V>
     */
    public TypeMirror getMapValueTypeMirror(TypeMirror type) {
        if (!isMapType(type)) return null;

        DeclaredType declaredType = (DeclaredType) type;
        if (declaredType.getTypeArguments().size() < 2) return null;

        return declaredType.getTypeArguments().get(1);
    }

    // ========== Enum field detection (Phase 1, PRP-28) ==========

    /** True if the type is an enum declaration (any enum — {@code @AsUuid}/{@code @AsName} presence is a separate question). */
    public boolean isEnumType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getKind() == ElementKind.ENUM;
    }

    /** True if the enum type carries {@code @AsUuid}. Only meaningful when {@link #isEnumType} is true. */
    public boolean isAsUuidEnum(TypeMirror type) {
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getAnnotation(AsUuid.class) != null;
    }

    /** True if the enum type carries {@code @AsName}. Only meaningful when {@link #isEnumType} is true. */
    public boolean isAsNameEnum(TypeMirror type) {
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getAnnotation(AsName.class) != null;
    }

    /**
     * True if the enum type carries {@code @EnumData}, so it owns a generated {@code Foo_I} and a
     * consumer can resolve a stored value through it rather than building its own lookup. Only
     * meaningful when {@link #isEnumType} is true.
     */
    public boolean isEnumDataEnum(TypeMirror type) {
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getAnnotation(EnumData.class) != null;
    }

    /**
     * The generated companion of an {@code @EnumData} enum: {@code Foo_I}, a top-level type beside
     * the enum. Correct for a nested enum too, whose companion is generated top-level in the same
     * package under the enum's own simple name.
     */
    public static ClassName enumDataInterface(TypeName enumType) {
        if (!(enumType instanceof ClassName cn)) return null;
        return ClassName.get(cn.packageName(), cn.simpleName() + "_I");
    }

    // ========== Reference (LINK) carrier detection ==========
    // Detected by fully-qualified name so the (generic) base processor stays decoupled from the
    // ArcadeDB module — same approach as isListType/isMapType.

    public static final String LINK_TYPE      = "datapotter.arcadedbhelper.Link";
    public static final String LINK_LIST_TYPE = "datapotter.arcadedbhelper.LinkList";
    public static final String LINK_MAP_TYPE  = "datapotter.arcadedbhelper.LinkMap";

    private boolean isQualifiedType(TypeMirror type, String qualifiedName) {
        if (type.getKind() != TypeKind.DECLARED) return false;
        TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
        return typeElement.getQualifiedName().toString().equals(qualifiedName);
    }

    /** True if the field type is {@code Link<T>}. */
    public boolean isLinkType(TypeMirror type) { return isQualifiedType(type, LINK_TYPE); }

    /** True if the field type is {@code LinkList<T>}. */
    public boolean isLinkListType(TypeMirror type) { return isQualifiedType(type, LINK_LIST_TYPE); }

    /** True if the field type is {@code LinkMap<K,T>}. */
    public boolean isLinkMapType(TypeMirror type) { return isQualifiedType(type, LINK_MAP_TYPE); }

    /** The target type {@code T} of {@code Link<T>} / {@code LinkList<T>} (type argument 0). */
    public TypeMirror getLinkTargetTypeMirror(TypeMirror type) {
        DeclaredType declaredType = (DeclaredType) type;
        return declaredType.getTypeArguments().isEmpty() ? null : declaredType.getTypeArguments().get(0);
    }

    /** The key type {@code K} of {@code LinkMap<K,T>} (type argument 0). */
    public TypeName getLinkMapKeyType(TypeMirror type) {
        DeclaredType declaredType = (DeclaredType) type;
        return declaredType.getTypeArguments().size() < 2 ? null
                : TypeName.get(declaredType.getTypeArguments().get(0));
    }

    /** The value type {@code T} of {@code LinkMap<K,T>} (type argument 1). */
    public TypeMirror getLinkMapValueTypeMirror(TypeMirror type) {
        DeclaredType declaredType = (DeclaredType) type;
        return declaredType.getTypeArguments().size() < 2 ? null : declaredType.getTypeArguments().get(1);
    }
}
