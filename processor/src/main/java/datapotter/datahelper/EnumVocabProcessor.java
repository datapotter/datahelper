package datapotter.datahelper;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import datapotter.datahelper.processor.util.EnumConstants;

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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
@SupportedAnnotationTypes({"datapotter.datahelper.AsUuid", "datapotter.datahelper.AsName"})
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
        // @EnumData generates a Foo_E that extends HasUuid and supplies uuid(), so the requirement is
        // met by construction. It also cannot be seen from here: Foo_E does not exist yet on this
        // round, and the original enum is not revisited on a later one.
        boolean generated = enumType.getAnnotation(EnumData.class) != null;
        if (!generated && !implementsHasUuid(enumType)) {
            error(enumType, "Enum '" + enumType.getSimpleName() + "' is annotated @AsUuid but does not "
                    + "implement HasUuid. Add 'implements HasUuid' and a uuid() accessor returning "
                    + "each constant's id, or add @EnumData to have both generated.");
            return;
        }

        UuidEncoding encoding = asUuid.value();
        Pattern alphabet = Pattern.compile(encoding.alphabetPattern());
        Map<String, String> seenLiteralToOwner = new HashMap<>();

        for (VariableElement constant : EnumConstants.of(enumType)) {
            String constantName = constant.getSimpleName().toString();
            String literal = EnumConstants.firstStringLiteral(trees, constant);

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

    /**
     * Whether {@code HasUuid} is reachable at all, not merely declared directly.
     *
     * <p>Was a scan of the direct interfaces only, which made a {@code HasUuid} inherited through a
     * shared vocabulary interface invisible and failed the build — so consumers worked around it by
     * having such an interface deliberately NOT extend {@code HasUuid} and naming both on every
     * enum. Nothing was gained by the narrowness: an enum that reaches {@code HasUuid} by any route
     * must still supply {@code uuid()}, and javac enforces that on its own.
     */
    private boolean implementsHasUuid(TypeElement enumType) {
        return reachesHasUuid(enumType.getInterfaces(), new HashSet<>());
    }

    private boolean reachesHasUuid(List<? extends TypeMirror> interfaces, Set<String> seen) {
        for (TypeMirror iface : interfaces) {
            String name = iface.toString();
            if (name.startsWith("datapotter.datahelper.HasUuid")) return true;
            if (!seen.add(name)) continue;
            Element resolved = processingEnv.getTypeUtils().asElement(iface);
            if (resolved instanceof TypeElement te && reachesHasUuid(te.getInterfaces(), seen)) return true;
        }
        return false;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
