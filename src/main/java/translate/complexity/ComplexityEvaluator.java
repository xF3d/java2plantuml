package translate.complexity;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import translate.component.MemberFormatter;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    interface System {
        ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<Node> allClazz, MemberFormatter formatter);
    }

    interface Clazz {
        ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz, MemberFormatter formatter);
    }

    interface Method {
        ComplexityMetricResult calculate(Node clazz, MethodDeclaration method, MemberFormatter formatter);
    }
}
