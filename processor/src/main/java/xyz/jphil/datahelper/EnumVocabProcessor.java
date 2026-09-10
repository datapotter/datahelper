package xyz.jphil.datahelper;

import com.google.auto.service.AutoService;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates every enum carrying {@code @AsUuid} or {@code @AsName} AT ITS OWN DECLARATION, whether
 * or not anything references it as a field yet. Backend-agnostic (see PRP-28 phase 1): a vocabulary
 * with stable identity is a data-modelling concept, not an ArcadeDB one — the ArcadeData processor
 * separately enforces the backend-specific rule that an unannotated enum cannot be stored.
 *
 * <p>Catches what is visible within a single compilation — malformed and duplicate ids — because a
 * processor claiming {@code @AsUuid}/{@code @AsName} sees every element carrying it, so a mistake
 * fails at the declaration rather than at some remote, possibly nonexistent, use site.
 *
 * <p><b>What this cannot catch, by design:</b> a uuid literal edited from one valid, unique value to
 * another compiles clean and orphans every stored row. The processor has no memory across builds;
 * only a committed golden-list guard in the consumer's own tests can catch that (see
 * {@code trials.CheckUuids} in the reference consumer, {@code uskoag-files-inventory}).
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"xyz.jphil.datahelper.AsUuid", "xyz.jphil.datahelper.AsName"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class EnumVocabProcessor extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.trees = Trees.instance(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Element> candidates = new LinkedHashSet<>();
        candidates.addAll(roundEnv.getElementsAnnotatedWith(AsUuid.class));
        candidates.addAll(roundEnv.getElementsAnnotatedWith(AsName.class));
        for (Element element : candidates) {
            validate(element);
        }
        return true;
    }

    private void validate(Element element) {
        if (element.getKind() != ElementKind.ENUM) {
            error(element, "@AsUuid/@AsName may only annotate an enum; '"
                    + element.getSimpleName() + "' is not one.");
            return;
        }
        TypeElement enumType = (TypeElement) element;
        AsUuid asUuid = enumType.getAnnotation(AsUuid.class);
        AsName asName = enumType.getAnnotation(AsName.class);

        if (asUuid != null && asName != null) {
            error(element, "Enum '" + enumType.getSimpleName() + "' carries both @AsUuid and @AsName. "
                    + "Storage form must be declared exactly once: pick @AsUuid(<encoding>) "
                    + "(and implement HasUuid) or @AsName, never both.");
            return;
        }

        if (asUuid != null) {
            validateAsUuid(enumType, asUuid);
        }
        // @AsName needs no further validation: name() is intrinsic to the enum, always present,
        // always unique within it — there is no literal to check and nothing to configure.
    }

    private void validateAsUuid(TypeElement enumType, AsUuid asUuid) {
        if (!implementsHasUuid(enumType)) {
            error(enumType, "Enum '" + enumType.getSimpleName() + "' is annotated @AsUuid but does not "
                    + "implement HasUuid. Add 'implements HasUuid' and a uuid() accessor returning "
                    + "each constant's id.");
            return;
        }

        UuidEncoding encoding = asUuid.value();
        Pattern alphabet = Pattern.compile(encoding.alphabetPattern());
        Map<String, String> seenLiteralToOwner = new HashMap<>();

        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.ENUM_CONSTANT) continue;
            VariableElement constant = (VariableElement) enclosed;
            String constantName = constant.getSimpleName().toString();
            String literal = extractUuidLiteral(constant);

            if (literal == null) {
                error(constant, "Could not read the uuid literal passed to '" + constantName + "'. "
                        + "@AsUuid requires the constant's first constructor argument to be a plain "
                        + "string literal (not a constant reference, concatenation, or computed expression).");
                continue;
            }

            if (literal.length() != encoding.length()) {
                error(constant, "'" + constantName + "' has uuid \"" + literal + "\" of length "
                        + literal.length() + ", but " + encoding + " requires exactly "
                        + encoding.length() + " characters.");
            } else if (!alphabet.matcher(literal).matches()) {
                error(constant, "'" + constantName + "' has uuid \"" + literal
                        + "\" which contains characters outside the " + encoding + " alphabet.");
            }

            String priorOwner = seenLiteralToOwner.putIfAbsent(literal, constantName);
            if (priorOwner != null) {
                error(constant, "'" + constantName + "' has the same uuid as '" + priorOwner
                        + "' (\"" + literal + "\"). Every constant in an @AsUuid enum must have a unique id.");
            }
        }
    }

    private boolean implementsHasUuid(TypeElement enumType) {
        for (TypeMirror iface : enumType.getInterfaces()) {
            if (iface.toString().startsWith("xyz.jphil.datahelper.HasUuid")) return true;
        }
        return false;
    }

    /**
     * Read the literal string passed as an enum constant's first constructor argument, directly
     * from source. {@code VariableElement.getConstantValue()} cannot do this — an enum constant is a
     * constructor call, not a compile-time constant expression in the JLS sense — so this walks the
     * javac Compiler Tree API instead. {@code com.sun.source.tree}/{@code .util} are part of
     * {@code jdk.compiler}'s ordinary exported surface (unlike {@code com.sun.tools.javac.*}
     * internals), so this needs no {@code --add-exports} and runs under plain {@code javac}.
     */
    private String extractUuidLiteral(VariableElement constant) {
        Tree tree = trees.getTree(constant);
        if (!(tree instanceof VariableTree vt)) return null;
        ExpressionTree init = vt.getInitializer();
        if (!(init instanceof NewClassTree nct)) return null;
        if (nct.getArguments().isEmpty()) return null;
        ExpressionTree arg0 = nct.getArguments().get(0);
        if (!(arg0 instanceof LiteralTree lit)) return null;
        Object value = lit.getValue();
        return value instanceof String s ? s : null;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
