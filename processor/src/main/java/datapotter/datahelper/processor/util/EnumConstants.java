package datapotter.datahelper.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Reading an enum's constants and their declared literals at compile time.
 *
 * <p>One copy, shared by the two processors that need it: {@code EnumVocabProcessor} validates the
 * uuid literals, {@code EnumDataProcessor} turns the same literals into a {@code fromStorage}
 * switch. Validation and generation must agree on what a constant's id is, so they read it the same
 * way.
 */
public final class EnumConstants {

    private EnumConstants() {}

    /** The enum's constants, in declaration order. */
    public static List<VariableElement> of(TypeElement enumType) {
        List<VariableElement> constants = new ArrayList<>();
        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                constants.add((VariableElement) enclosed);
            }
        }
        return constants;
    }

    /**
     * The string literal passed as a constant's first constructor argument, or {@code null} if the
     * argument is absent or is anything other than a plain literal.
     *
     * <p>{@code VariableElement.getConstantValue()} cannot do this — an enum constant is a
     * constructor call, not a compile-time constant expression in the JLS sense — so this walks the
     * javac Compiler Tree API instead. {@code com.sun.source.tree}/{@code .util} are part of
     * {@code jdk.compiler}'s ordinary exported surface (unlike {@code com.sun.tools.javac.*}
     * internals), so this needs no {@code --add-exports} and runs under plain {@code javac}.
     */
    public static String firstStringLiteral(Trees trees, VariableElement constant) {
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
}
