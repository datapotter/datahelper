package datapotter.datahelper;

import com.google.auto.service.AutoService;
import datapotter.datahelper.processor.util.PropertyIds;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates every field carrying {@code @P} AT ITS OWN DECLARATION (PRP-28 phase 2), the same
 * discipline {@link EnumVocabProcessor} already applies to {@code @AsUuid}/{@code @AsName}: a
 * processor claiming the annotation sees every field carrying it in this compilation, so a malformed
 * or duplicate id fails here rather than at some remote migration, or never if nobody notices.
 *
 * <p>Backend-agnostic on purpose — an id's FORMAT is a property-identity concept, not an ArcadeDB
 * one (see {@code 28-prp.04} section 1). A backend-specific completeness rule such as
 * {@code @ArcadeData(requireIds = true)} is enforced by that backend's own processor, which calls
 * into {@link PropertyIds} for minting rather than carrying a second implementation.
 *
 * <p><b>What this catches:</b> wrong length, a character outside the alphabet, a bad check
 * character (with the six single-character repairs listed), and two fields of the same enclosing
 * type sharing one id. <b>What it cannot catch, by design:</b> an id edited from one valid, unique,
 * correctly-checksummed value to another — that has no signal visible within one compilation. Only a
 * committed, append-only id list (this project's analogue of {@code trials.CheckUuids}) catches it.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("datapotter.datahelper.P")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class PropertyIdProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Grouped by enclosing type, because uniqueness (DP-ID-004) is scoped to one type, not the
        // whole compilation — the ONLY thing that can collide across two unrelated classes safely.
        Map<Element, Map<String, String>> idsByEnclosingType = new LinkedHashMap<>();

        for (Element field : roundEnv.getElementsAnnotatedWith(P.class)) {
            P p = field.getAnnotation(P.class);
            String id = p.value();
            String fieldName = field.getSimpleName().toString();

            PropertyIds.ValidationResult result = PropertyIds.validate(id);
            if (!result.valid()) {
                error(field, formatDiagnostic(fieldName, id, result));
                continue; // A malformed id can't usefully be checked for duplication.
            }

            Element enclosing = field.getEnclosingElement();
            Map<String, String> seen = idsByEnclosingType.computeIfAbsent(enclosing, k -> new HashMap<>());
            String priorField = seen.putIfAbsent(id, fieldName);
            if (priorField != null) {
                error(field, "DP-ID-004: field '" + fieldName + "' has the same id ('" + id
                        + "') as field '" + priorField + "' in " + enclosing.getSimpleName()
                        + ". Every @P in one type must be unique — mint a new one with "
                        + "'datapotter-id new'.");
            }
        }
        return true;
    }

    private String formatDiagnostic(String fieldName, String id, PropertyIds.ValidationResult result) {
        String code = switch (result.problem()) {
            case WRONG_LENGTH -> "DP-ID-001";
            case BAD_CHARACTER -> "DP-ID-002";
            case BAD_CHECK -> "DP-ID-003";
        };
        String message = code + ": field '" + fieldName + "' has @P(\"" + id + "\") — " + result.message();
        // DP-ID-003 lists all six single-character repairs (28-prp.04 section 6) — never just the
        // one that assumes the check character itself was the typo, since that guess is wrong
        // exactly when the mistake was in one of the first five characters.
        if (!result.repairCandidates().isEmpty()) {
            message += " Candidates: " + String.join(", ", result.repairCandidates());
        }
        return message;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
