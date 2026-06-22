package xyz.jphil.datahelper;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import xyz.jphil.datahelper.processor.util.FieldAnalyzer;
import xyz.jphil.datahelper.processor.util.FieldInfo;
import xyz.jphil.datahelper.processor.util.ProcessorUtils;
import xyz.jphil.datahelper.processor.util.ProjectionGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor for {@code @DataHelper} (Lombok-companion path).
 *
 * <p>Generates, per annotated class {@code Foo}:
 * <ul>
 *   <li>{@code Foo_IR} — readable interface ($symbols, FIELDS, typed getters, read accessors, {@code toRecord()})</li>
 *   <li>{@code Foo_I extends Foo_IR, DataHelper_I} — adds setters + write accessors</li>
 *   <li>{@code Foo_R implements Foo_IR<Foo_R>} — immutable record projection</li>
 * </ul>
 *
 * <p>The user class implements {@code Foo_I<Foo>} and provides field getters/setters (Lombok).
 * Optional {@code superInterfaces} are split across {@code _IR}/{@code _I} by naming convention,
 * so a trait's read half (e.g. {@code Json_IR}, giving {@code toJson}) lands on {@code _IR} and is
 * inherited by the record, while its write half ({@code Json_I}, giving {@code fromJson}) lands on
 * {@code _I} (mutable only).</p>
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
                    generateProjection((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void generateProjection(TypeElement element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).toString();
        String className = element.getSimpleName().toString();

        ProcessorUtils utils = new ProcessorUtils(processingEnv);
        FieldAnalyzer analyzer = new FieldAnalyzer(processingEnv, utils);

        List<FieldInfo> fields = analyzer.analyzeFields(element);
        if (fields == null) {
            return; // Validation errors found
        }

        // Split declared superInterfaces (traits) across the read/write interfaces by convention.
        List<TypeName> irSupers = new ArrayList<>();
        List<TypeName> iSupers = new ArrayList<>();
        wireSuperInterfaces(element, irSupers, iSupers);

        TypeSpec ir = ProjectionGenerator.buildReadableInterface(packageName, className, fields, utils, irSupers);
        TypeSpec i  = ProjectionGenerator.buildWritableInterface(packageName, className, fields, utils, iSupers);
        TypeSpec r  = ProjectionGenerator.buildRecord(packageName, className, fields);

        writeType(packageName, ir, className + "_IR");
        writeType(packageName, i,  className + "_I");
        writeType(packageName, r,  className + "_R");
    }

    /**
     * Resolve {@code @DataHelper(superInterfaces = {...})} and route each declared trait to the
     * readable and/or writable interface. A trait declared as its {@code _IR}/{@code _I} half also
     * pulls in its sibling half (resolved by naming convention) when present on the classpath; a
     * plain marker interface is attached to the writable (mutable) interface only.
     */
    private void wireSuperInterfaces(TypeElement element, List<TypeName> irSupers, List<TypeName> iSupers) {
        DataHelper ann = element.getAnnotation(DataHelper.class);
        if (ann == null) return;

        List<? extends TypeMirror> declared;
        try {
            ann.superInterfaces();
            return; // no MirroredTypesException -> empty
        } catch (MirroredTypesException mte) {
            declared = mte.getTypeMirrors();
        }

        for (TypeMirror t : declared) {
            TypeElement te = (TypeElement) processingEnv.getTypeUtils().asElement(t);
            if (te == null) continue;

            String simpleName = te.getSimpleName().toString();
            String pkg = processingEnv.getElementUtils().getPackageOf(te).getQualifiedName().toString();

            if (simpleName.endsWith("_IR")) {
                irSupers.add(asSuper(pkg, simpleName, te));
                String writeName = simpleName.substring(0, simpleName.length() - 3) + "_I"; // _IR -> _I
                addIfPresent(pkg, writeName, iSupers);
            } else if (simpleName.endsWith("_I")) {
                iSupers.add(asSuper(pkg, simpleName, te));
                String readName = simpleName.substring(0, simpleName.length() - 2) + "_IR"; // _I -> _IR
                addIfPresent(pkg, readName, irSupers);
            } else {
                // Plain marker (e.g. a JS interop marker): mutable side only, keep records clean.
                iSupers.add(asSuper(pkg, simpleName, te));
            }
        }
    }

    /** A trait super-interface, parameterized with {@code E} when it declares a type variable. */
    private TypeName asSuper(String pkg, String simpleName, TypeElement te) {
        ClassName cn = ClassName.get(pkg, simpleName);
        return te.getTypeParameters().isEmpty()
                ? cn
                : ParameterizedTypeName.get(cn, TypeVariableName.get("E"));
    }

    private void addIfPresent(String pkg, String simpleName, List<TypeName> target) {
        TypeElement sibling = processingEnv.getElementUtils().getTypeElement(pkg + "." + simpleName);
        if (sibling != null) {
            target.add(asSuper(pkg, simpleName, sibling));
        }
    }

    private void writeType(String packageName, TypeSpec type, String displayName) {
        JavaFile javaFile = JavaFile.builder(packageName, type)
                .indent("    ")
                .skipJavaLangImports(true)
                .addFileComment("Generated by DataHelperProcessor on " + LocalDateTime.now())
                .build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generated: " + displayName);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate " + displayName + ": " + e.getMessage());
        }
    }
}
