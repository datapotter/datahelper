package xyz.jphil.datahelper.processor.util;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import com.palantir.javapoet.WildcardTypeName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the readable/writable interface split and the immutable record projection that are
 * common to both annotation paths:
 *
 * <ul>
 *   <li>{@code Foo_IR} — readable interface: {@code $symbols}, {@code FIELDS}, typed getters
 *       (widened to the readable interface for generated DataHelper components), read-side
 *       property accessors, and the abstract {@code toRecord()}.</li>
 *   <li>{@code Foo_I extends Foo_IR, DataHelper_I} — writable interface: setters, write-side
 *       property accessors, concrete-narrowed getters for the widened fields, and a default
 *       {@code toRecord()} that builds the record (deep).</li>
 *   <li>{@code Foo_R implements Foo_IR<Foo_R>} — immutable record projection: components (nested
 *       generated DataHelpers mapped to their {@code _R}), {@code getXxx()} bridges, a
 *       {@code dataClass()} override returning {@code Foo_R.class} (keeps equality symmetric with
 *       the mutable), identity {@code toRecord()}, and a deep {@code toMutable()}.</li>
 * </ul>
 *
 * <p>Widening + record mapping apply only to <em>annotation-generated</em> nested DataHelpers
 * (which have generated {@code _IR}/{@code _R} siblings). Hand-written nested DataHelpers degrade
 * to their concrete type with identity conversion.</p>
 */
public final class ProjectionGenerator {

    private static final String DH = "xyz.jphil.datahelper";

    private ProjectionGenerator() {}

    // ====================================================================== type derivation

    /** Derive a generated sibling type (e.g. {@code AddressDTO -> AddressDTO_R}) in its own package. */
    public static ClassName sibling(TypeName concrete, String suffix) {
        ClassName cn = rawClassName(concrete);
        return ClassName.get(cn.packageName(), cn.simpleName() + suffix);
    }

    private static ClassName rawClassName(TypeName t) {
        if (t instanceof ParameterizedTypeName p) return p.rawType();
        return (ClassName) t;
    }

    /** {@code Foo_IR<?>} */
    private static TypeName irWildcard(TypeName concrete) {
        return ParameterizedTypeName.get(sibling(concrete, "_IR"), WildcardTypeName.subtypeOf(Object.class));
    }

    /**
     * Return type of a getter on the readable {@code _IR}. Widened for generated DataHelper
     * components so both the mutable (concrete) and the record ({@code _R}) covariantly implement it.
     */
    public static TypeName readableGetterType(FieldInfo f) {
        if (f.isNestedDataHelper && f.isNestedGenerated) {
            return irWildcard(f.type);
        }
        if (f.isListOfDataHelper && f.isListElementGenerated) {
            return ParameterizedTypeName.get(ClassName.get(List.class),
                    WildcardTypeName.subtypeOf(irWildcard(f.listElementType)));
        }
        if (f.isMapOfDataHelper && f.isMapValueGenerated) {
            return ParameterizedTypeName.get(ClassName.get(Map.class),
                    f.mapKeyType, WildcardTypeName.subtypeOf(irWildcard(f.mapValueType)));
        }
        return f.type;
    }

    /** True when {@link #readableGetterType} widened the field, so {@code _I} must re-narrow it. */
    public static boolean isWidened(FieldInfo f) {
        return (f.isNestedDataHelper && f.isNestedGenerated)
            || (f.isListOfDataHelper && f.isListElementGenerated)
            || (f.isMapOfDataHelper && f.isMapValueGenerated);
    }

    /** Record component type: simple as-declared, generated DataHelper {@code -> _R} (deep), hand-written {@code -> } concrete. */
    public static TypeName recordComponentType(FieldInfo f) {
        if (f.isNestedDataHelper) {
            return f.isNestedGenerated ? sibling(f.type, "_R") : f.type;
        }
        if (f.isListOfDataHelper) {
            TypeName elem = f.isListElementGenerated ? sibling(f.listElementType, "_R") : f.listElementType;
            return ParameterizedTypeName.get(ClassName.get(List.class), elem);
        }
        if (f.isMapOfDataHelper) {
            TypeName val = f.isMapValueGenerated ? sibling(f.mapValueType, "_R") : f.mapValueType;
            return ParameterizedTypeName.get(ClassName.get(Map.class), f.mapKeyType, val);
        }
        return f.type;
    }

    private static String getterName(FieldInfo f) {
        return (ProcessorUtils.isBooleanType(f.type) ? "is" : "get") + ProcessorUtils.capitalize(f.name);
    }

    private static String setterName(FieldInfo f) {
        return "set" + ProcessorUtils.capitalize(f.name);
    }

    private static TypeName classWildcard() {
        return ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
    }

    // ====================================================================== _IR (readable)

    public static TypeSpec buildReadableInterface(String pkg, String className, List<FieldInfo> fields,
                                                  ProcessorUtils utils, List<TypeName> extraSupers) {
        ClassName irCn = ClassName.get(pkg, className + "_IR");
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(className + "_IR")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("E", ParameterizedTypeName.get(irCn, TypeVariableName.get("E"))))
                .addJavadoc("Readable projection of {@link $L}: shared by the mutable type and the immutable {@link $L_R} record.\n",
                        className, className)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(DH, "DataHelper_IR"), TypeVariableName.get("E")));
        for (TypeName s : extraSupers) b.addSuperinterface(s);

        CodeGeneratorUtils.addFieldSymbols(b, fields, pkg, className, "_IR");
        CodeGeneratorUtils.addFieldsList(b, fields, pkg, className);

        addReadableGetters(b, fields);
        addReadableFluentGetters(b, fields);

        b.addMethod(CodeGeneratorUtils.createDataClassMethod(pkg, className, true));
        b.addMethod(CodeGeneratorUtils.createFieldNamesMethod(true));
        b.addMethod(CodeGeneratorUtils.createGetPropertyByNameMethod(fields, utils, true));
        b.addMethod(CodeGeneratorUtils.createGetPropertyTypeMethod(fields, true));
        b.addMethod(CodeGeneratorUtils.createIsListFieldMethod(fields, true));
        b.addMethod(CodeGeneratorUtils.createIsNestedObjectFieldMethod(fields, true));
        CodeGeneratorUtils.addMapReadMethods(b, fields, true);

        b.addMethod(MethodSpec.methodBuilder("toRecord")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(pkg, className + "_R"))
                .addJavadoc("Project to the immutable {@link $L_R} record (deep).\n", className)
                .build());

        return b.build();
    }

    private static void addReadableGetters(TypeSpec.Builder b, List<FieldInfo> fields) {
        for (FieldInfo f : fields) {
            String cap = ProcessorUtils.capitalize(f.name);
            if (ProcessorUtils.isBooleanType(f.type)) {
                b.addMethod(MethodSpec.methodBuilder("is" + cap)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(f.type).build());
                b.addMethod(MethodSpec.methodBuilder("get" + cap)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).returns(f.type)
                        .addStatement("return is$L()", cap).build());
            } else {
                b.addMethod(MethodSpec.methodBuilder("get" + cap)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(readableGetterType(f)).build());
            }
        }
    }

    private static void addReadableFluentGetters(TypeSpec.Builder b, List<FieldInfo> fields) {
        for (FieldInfo f : fields) {
            b.addMethod(MethodSpec.methodBuilder(f.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(readableGetterType(f))
                    .addStatement("return $L()", getterName(f))
                    .build());
        }
    }

    // ====================================================================== _I (writable)

    public static TypeSpec buildWritableInterface(String pkg, String className, List<FieldInfo> fields,
                                                  ProcessorUtils utils, List<TypeName> extraSupers) {
        ClassName iCn = ClassName.get(pkg, className + "_I");
        TypeSpec.Builder b = TypeSpec.interfaceBuilder(className + "_I")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("E", ParameterizedTypeName.get(iCn, TypeVariableName.get("E"))))
                .addJavadoc("Full read+write interface for {@link $L}. Extends the readable {@link $L_IR}.\n",
                        className, className)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(pkg, className + "_IR"), TypeVariableName.get("E")))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(DH, "DataHelper_I"), TypeVariableName.get("E")));
        for (TypeName s : extraSupers) b.addSuperinterface(s);

        addNarrowedReadAccessors(b, fields);
        addWritableSetters(b, fields);

        b.addMethod(CodeGeneratorUtils.createSetPropertyByNameMethod(fields, utils, true));
        b.addMethod(CodeGeneratorUtils.createNestedObjectMethod(fields, true));
        b.addMethod(CodeGeneratorUtils.createListElementMethod(fields, true));
        CodeGeneratorUtils.addMapWriteMethods(b, fields, true);

        b.addMethod(buildToRecordDefault(pkg, className, fields));
        return b.build();
    }

    /** For widened fields, re-declare the getter (and fluent getter) at the concrete type on {@code _I}. */
    private static void addNarrowedReadAccessors(TypeSpec.Builder b, List<FieldInfo> fields) {
        for (FieldInfo f : fields) {
            if (!isWidened(f)) continue;
            String g = getterName(f); // never boolean (DataHelper-typed)
            b.addMethod(MethodSpec.methodBuilder(g)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(f.type).build());
            b.addMethod(MethodSpec.methodBuilder(f.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).returns(f.type)
                    .addStatement("return $L()", g).build());
        }
    }

    private static void addWritableSetters(TypeSpec.Builder b, List<FieldInfo> fields) {
        AnnotationSpec unchecked = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build();
        for (FieldInfo f : fields) {
            b.addMethod(MethodSpec.methodBuilder(setterName(f))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(f.type, f.name).build());
            b.addMethod(MethodSpec.methodBuilder(f.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotation(unchecked)
                    .addParameter(f.type, f.name)
                    .returns(TypeVariableName.get("E"))
                    .addStatement("$L($N)", setterName(f), f.name)
                    .addStatement("return (E) this")
                    .build());
        }
    }

    private static MethodSpec buildToRecordDefault(String pkg, String className, List<FieldInfo> fields) {
        ClassName recordCn = ClassName.get(pkg, className + "_R");
        CodeBlock.Builder args = CodeBlock.builder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) args.add(",\n    ");
            args.add(toRecordArg(fields.get(i)));
        }
        return MethodSpec.methodBuilder("toRecord")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addAnnotation(Override.class)
                .returns(recordCn)
                .addStatement("return new $T($L)", recordCn, args.build())
                .build();
    }

    private static CodeBlock toRecordArg(FieldInfo f) {
        String g = getterName(f);
        if (f.isNestedDataHelper) {
            return f.isNestedGenerated
                    ? CodeBlock.of("$1L() == null ? null : $1L().toRecord()", g)
                    : CodeBlock.of("$L()", g);
        }
        if (f.isListField) {
            if (f.isListOfDataHelper && f.isListElementGenerated) {
                return CodeBlock.of("$1L() == null ? null : $1L().stream().map($2T::toRecord).toList()", g, f.listElementType);
            }
            return CodeBlock.of("$1L() == null ? null : $2T.copyOf($1L())", g, ClassName.get(List.class));
        }
        if (f.isMapField) {
            if (f.isMapOfDataHelper && f.isMapValueGenerated) {
                return CodeBlock.of(
                        "$1L() == null ? null : $1L().entrySet().stream().collect($2T.toMap($3T::getKey, e -> e.getValue().toRecord(), (a, b) -> b, $4T::new))",
                        g, ClassName.get(Collectors.class), ClassName.get(Map.Entry.class), ClassName.get(LinkedHashMap.class));
            }
            return CodeBlock.of("$1L() == null ? null : $2T.copyOf($1L())", g, ClassName.get(Map.class));
        }
        return CodeBlock.of("$L()", g);
    }

    // ====================================================================== _R (record)

    public static TypeSpec buildRecord(String pkg, String className, List<FieldInfo> fields) {
        ClassName recordCn = ClassName.get(pkg, className + "_R");
        ClassName irCn = ClassName.get(pkg, className + "_IR");
        ClassName concrete = ClassName.get(pkg, className);

        MethodSpec.Builder ctor = MethodSpec.constructorBuilder();
        for (FieldInfo f : fields) {
            ctor.addParameter(recordComponentType(f), f.name);
        }

        TypeSpec.Builder b = TypeSpec.recordBuilder(className + "_R")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Immutable record projection of {@link $L} (deep). Shares the readable {@link $L_IR}.\n",
                        className, className)
                .addSuperinterface(ParameterizedTypeName.get(irCn, recordCn))
                .recordConstructor(ctor.build());

        // getXxx() / isXxx() bridges so the record satisfies the _IR getter contract.
        for (FieldInfo f : fields) {
            b.addMethod(MethodSpec.methodBuilder(getterName(f))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(recordComponentType(f))
                    .addStatement("return $L", f.name)
                    .build());
        }

        b.addMethod(MethodSpec.methodBuilder("dataClass")
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .returns(classWildcard())
                .addStatement("return $T.class", recordCn)
                .build());

        b.addMethod(MethodSpec.methodBuilder("toRecord")
                .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                .returns(recordCn)
                .addStatement("return this")
                .build());

        b.addMethod(buildToMutable(concrete, fields));
        return b.build();
    }

    // ====================================================================== _A (@Data delegating base)

    /**
     * Add concrete field-backed getters/setters (delegating to {@code sub}) for the {@code @Data}
     * sealed {@code _A} base. The property accessors, fluent methods, and {@code toRecord()} are
     * inherited as defaults from {@code _IR}/{@code _I}, so {@code _A} only contributes these and
     * the {@code Object} methods.
     */
    public static void addDelegatingAccessors(TypeSpec.Builder b, List<FieldInfo> fields) {
        for (FieldInfo f : fields) {
            String cap = ProcessorUtils.capitalize(f.name);
            String name = (ProcessorUtils.isBooleanType(f.type) ? "is" : "get") + cap;
            b.addMethod(MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC).returns(f.type)
                    .addStatement("return sub.$N", f.name).build());
        }
        for (FieldInfo f : fields) {
            b.addMethod(MethodSpec.methodBuilder(setterName(f))
                    .addModifiers(Modifier.PUBLIC).addParameter(f.type, f.name)
                    .addStatement("sub.$N = $N", f.name, f.name).build());
        }
    }

    /** {@code public static Foo from(Foo_R r)} — inheritable as {@code Foo.from(...)} on the @Data path. */
    public static MethodSpec buildFromStatic(String pkg, String className) {
        ClassName concrete = ClassName.get(pkg, className);
        return MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(concrete)
                .addParameter(ClassName.get(pkg, className + "_R"), "r")
                .addJavadoc("Materialize a mutable $T from its immutable {@link $L_R} projection.\n", concrete, className)
                .addStatement("return r == null ? null : r.toMutable()")
                .build();
    }

    private static MethodSpec buildToMutable(ClassName concrete, List<FieldInfo> fields) {
        MethodSpec.Builder m = MethodSpec.methodBuilder("toMutable")
                .addModifiers(Modifier.PUBLIC)
                .returns(concrete)
                .addJavadoc("Materialize a mutable $T from this immutable projection (deep).\n", concrete)
                .addStatement("$1T m = new $1T()", concrete);
        for (FieldInfo f : fields) {
            m.addStatement("$L", toMutableArg(f));
        }
        m.addStatement("return m");
        return m.build();
    }

    private static CodeBlock toMutableArg(FieldInfo f) {
        String s = setterName(f);
        String n = f.name;
        if (f.isNestedDataHelper) {
            return f.isNestedGenerated
                    ? CodeBlock.of("m.$1L($2L == null ? null : $2L.toMutable())", s, n)
                    : CodeBlock.of("m.$1L($2L)", s, n);
        }
        if (f.isListField) {
            if (f.isListOfDataHelper && f.isListElementGenerated) {
                return CodeBlock.of("m.$1L($2L == null ? null : $2L.stream().map($3T::toMutable).collect($4T.toList()))",
                        s, n, sibling(f.listElementType, "_R"), ClassName.get(Collectors.class));
            }
            return CodeBlock.of("m.$1L($2L == null ? null : new $3T<>($2L))", s, n, ClassName.get(ArrayList.class));
        }
        if (f.isMapField) {
            if (f.isMapOfDataHelper && f.isMapValueGenerated) {
                return CodeBlock.of(
                        "m.$1L($2L == null ? null : $2L.entrySet().stream().collect($3T.toMap($4T::getKey, e -> e.getValue().toMutable(), (a, b) -> b, $5T::new)))",
                        s, n, ClassName.get(Collectors.class), ClassName.get(Map.Entry.class), ClassName.get(LinkedHashMap.class));
            }
            return CodeBlock.of("m.$1L($2L == null ? null : new $3T<>($2L))", s, n, ClassName.get(LinkedHashMap.class));
        }
        return CodeBlock.of("m.$1L($2L)", s, n);
    }
}
