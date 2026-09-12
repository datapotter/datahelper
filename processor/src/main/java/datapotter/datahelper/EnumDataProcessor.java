package datapotter.datahelper;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

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
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor for {@code @EnumData}: generates the sealed {@code Foo_I} interface carrying
 * one {@code default} accessor per instance field, so an enum's per-field getters stop being
 * hand-written.
 *
 * <p>The {@code @Data} path hangs its accessors on a generated base class the type extends. An enum
 * already extends {@code Enum}, so here they arrive as interface defaults reading through
 * {@link EnumData_I#self()} — which the generated interface implements itself, by a cast its own
 * {@code sealed}/{@code permits} makes total.
 *
 * <p><b>Traits.</b> Interfaces named on {@code @EnumData(traits = ...)} become supertypes of
 * {@code Foo_I}, so the generated defaults satisfy their abstract methods. They must land there
 * rather than beside the enum: an abstract method inherited from one interface and a
 * {@code default} inherited from an unrelated one is a compile error (JLS 9.4.1.3).
 *
 * <p><b>{@code @AsUuid}/{@code @AsName} integration.</b> Where either is present the generated
 * interface also gets {@code fromStorage(String)} — the reverse of the write-side
 * {@link HasUuid#storageValue(Object)} — backed by a lazily initialized nested holder.
 *
 * <p><b>Generated code never restates an id.</b> Every key is read from the constant itself, so the
 * enum source remains the single place any id exists. Generated output is derived and disposable,
 * and an id copied into it would be a second home for the one piece of data here that must never
 * quietly change.
 *
 * @see EnumData
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("datapotter.datahelper.EnumData")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class EnumDataProcessor extends AbstractProcessor {

    private static final String DH = "datapotter.datahelper";

    /** Nested holder and its field — named once so the emitted call and the class cannot drift. */
    private static final String LOOKUP = "Lookup";
    private static final String INDEX = "BY_STORED_ID";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(EnumData.class)) {
            if (element.getKind() != ElementKind.ENUM) {
                error(element, "@EnumData may only annotate an enum; '" + element.getSimpleName()
                        + "' is not one. For a class or record, use @Data.");
                continue;
            }
            generate((TypeElement) element);
        }
        return true;
    }

    private void generate(TypeElement enumType) {
        String pkg = processingEnv.getElementUtils().getPackageOf(enumType).toString();
        String className = enumType.getSimpleName().toString();
        String ifaceName = className + "_I";
        ClassName concrete = ClassName.get(pkg, className);

        List<VariableElement> fields = instanceFields(enumType, ifaceName);
        if (fields == null) return;

        AsUuid asUuid = enumType.getAnnotation(AsUuid.class);
        AsName asName = enumType.getAnnotation(AsName.class);

        TypeSpec.Builder b = TypeSpec.interfaceBuilder(ifaceName)
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addPermittedSubclass(concrete)
                .addJavadoc("Generated accessors for {@link $L} (@EnumData).\n", className)
                .addJavadoc("\n<p>Sealed on $L alone, which is what lets {@link #self()} be supplied here\n", className)
                .addJavadoc("rather than written in the enum.</p>\n");

        List<TypeMirror> traits = declaredTraits(enumType);
        Set<String> supers = new LinkedHashSet<>();
        for (TypeMirror trait : traits) {
            if (supers.add(trait.toString())) b.addSuperinterface(TypeName.get(trait));
        }
        // ALWAYS, when @AsUuid is present — never merely when HasUuid is not already reachable
        // through a trait. Reaching it transitively makes the enum's identity contract depend on
        // someone else's declaration staying put: let a trait stop extending HasUuid and this enum
        // silently stops being one, with nothing failing to compile (uuid() survives as an ordinary
        // field accessor) and EnumVocabProcessor's check skipped on the grounds that generation
        // guarantees it. HasUuid.storageValue would then take the plain-Enum branch and write name()
        // where a uuid belongs. Naming it directly costs nothing: an interface may extend both a
        // supertype and its subtype. The set only guards against naming the SAME one twice, which
        // is the one arrangement javac rejects.
        if (asUuid != null && supers.add(DH + ".HasUuid")) {
            b.addSuperinterface(ClassName.get(DH, "HasUuid"));
        }
        b.addSuperinterface(ParameterizedTypeName.get(ClassName.get(DH, "EnumData_I"), concrete));

        b.addMethod(MethodSpec.methodBuilder("self")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(concrete)
                .addJavadoc("This constant, typed as $L. Total by construction: this interface is\n", className)
                .addJavadoc("sealed and permits only $L.\n", className)
                .addStatement("return ($T) this", concrete)
                .build());

        for (VariableElement f : fields) {
            String name = f.getSimpleName().toString();
            b.addMethod(MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(TypeName.get(f.asType()))
                    .addStatement("return self().$N", name)
                    .build());
        }

        // Nothing here identifies "the uuid field", and nothing requires one to be named anything.
        // A field called uuid yields uuid() like any other; an enum that stores its id elsewhere
        // writes the one-line accessor itself. If neither happens, javac says so against the enum —
        // the right place — because Foo_I extends HasUuid and leaves uuid() abstract.
        if (asUuid != null || asName != null) {
            b.addMethod(buildFromStorage(concrete));
            b.addType(buildLookupHolder(concrete, asUuid != null ? "uuid" : "name"));
        }

        write(pkg, b.build(), ifaceName);
    }

    /**
     * The enum's instance fields, or {@code null} if any is unusable. A {@code private} field cannot
     * be read by an interface default, so it is an error here rather than a confusing failure inside
     * generated code — the same package-private requirement {@code @Data} places on a child class.
     */
    private List<VariableElement> instanceFields(TypeElement enumType, String ifaceName) {
        List<VariableElement> fields = new ArrayList<>();
        boolean failed = false;
        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            if (enclosed.getModifiers().contains(Modifier.STATIC)) continue;
            if (enclosed.getModifiers().contains(Modifier.PRIVATE)) {
                error(enclosed, "Field '" + enclosed.getSimpleName() + "' is private, so the generated "
                        + ifaceName + " cannot read it. Drop the 'private' modifier: package-private "
                        + "is the narrowest that works, and is what @Data requires of its fields too.");
                failed = true;
                continue;
            }
            fields.add((VariableElement) enclosed);
        }
        return failed ? null : fields;
    }

    /**
     * The traits named on {@code @EnumData(traits = ...)}.
     *
     * <p>Declared rather than inferred from the enum's {@code implements} clause. Inferring worked,
     * but it made the enum's own declaration the INPUT to its generated supertype — so the clause
     * read as redundant once {@code Foo_I} extended the same interface, and deleting it as a tidy-up
     * silently stopped the enum being a {@code Described}. Naming the trait here leaves the enum
     * implementing its generated interface and nothing else.
     *
     * <p>Reading a {@code Class} out of an annotation during processing always throws
     * {@link MirroredTypesException} — the class need not exist as a loadable {@code Class} at all
     * — and the mirrors it carries are the answer, not a failure.
     */
    private List<TypeMirror> declaredTraits(TypeElement enumType) {
        try {
            enumType.getAnnotation(EnumData.class).traits();
            return List.of();                       // no traits declared
        } catch (MirroredTypesException expected) {
            return List.copyOf(expected.getTypeMirrors());
        }
    }

    /**
     * The stored-string-to-constant lookup, on the enum itself instead of on each consumer.
     *
     * <p>Consumers used to build one map per enum field per entity and decide {@code uuid} versus
     * {@code name} themselves; with this generated, a consumer calls {@code Foo_I.fromStorage} and
     * never learns the storage form at all.
     */
    private MethodSpec buildFromStorage(ClassName concrete) {
        return MethodSpec.methodBuilder("fromStorage")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(concrete)
                .addParameter(String.class, "storedValue")
                .addJavadoc("The constant a stored id resolves to, or null.\n")
                .addJavadoc("\n<p>Never throws: a value matching no constant was written by newer code\n")
                .addJavadoc("than this build, which is not the same as corrupt.</p>\n")
                .addStatement("return storedValue == null ? null : $N.$N.get(storedValue)",
                        LOOKUP, INDEX)
                .build();
    }

    /**
     * The index behind {@link #buildFromStorage}, in a nested holder so it initializes LAZILY.
     *
     * <p><b>NO ID LITERAL IS EVER WRITTEN INTO GENERATED CODE.</b> Keys come from {@code c.uuid()}
     * (or {@code c.name()}), so the enum source stays the single place any id exists. Generated
     * output is derived and disposable — erased by a clean, rewritten by the IDE — and an id copied
     * into it would be a second home for the one piece of data in this model that must never quietly
     * change, in a file no golden-list guard reads.
     *
     * <p><b>Why a nested class and not a field on the interface.</b> A {@code Map} field here would
     * be initialized by the interface's own class init, which is triggered by the ENUM's, so
     * {@code Foo.values()} would be read back while {@code Foo} was still initializing and come back
     * null. Measured, not theorised: it threw {@code ExceptionInInitializerError} on first use. A
     * nested class initializes on first access instead, which is inside {@code fromStorage} — a
     * call, therefore after the enum is built.
     *
     * <p>{@code values()} is the enum's own synthetic method, not {@code getEnumConstants()}, so
     * this stays reflection-free for the TeaVM target. One map per enum, built once, on demand.
     */
    private TypeSpec buildLookupHolder(ClassName concrete, String keyAccessor) {
        ParameterizedTypeName mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.get(String.class), concrete);

        // public/static are implicit for a nested type in an interface, but JavaPoet wants them said.
        return TypeSpec.classBuilder(LOOKUP)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Lazily built index for {@link $L#fromStorage}. Holds no id literal:\n",
                        concrete.simpleName() + "_I")
                .addJavadoc("every key is read from the constant itself.\n")
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(mapType, INDEX, Modifier.STATIC, Modifier.FINAL)
                        .initializer("build()")
                        .build())
                .addMethod(MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .returns(mapType)
                        .addStatement("var m = new $T<$T, $T>()",
                                ClassName.get(HashMap.class), ClassName.get(String.class), concrete)
                        .addStatement("for ($T c : $T.values()) m.put(c.$N(), c)",
                                concrete, concrete, keyAccessor)
                        .addStatement("return $T.copyOf(m)", ClassName.get(Map.class))
                        .build())
                .build();
    }

    private void write(String pkg, TypeSpec type, String displayName) {
        JavaFile javaFile = JavaFile.builder(pkg, type)
                .indent("    ")
                .skipJavaLangImports(true)
                .addFileComment("Generated by EnumDataProcessor on " + LocalDateTime.now())
                .build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generated: " + displayName);
        } catch (IOException e) {
            error(null, "Failed to generate " + displayName + ": " + e.getMessage());
        }
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
