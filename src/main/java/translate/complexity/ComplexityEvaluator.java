package translate.complexity;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import translate.complexity.clazz.DITEvaluator;
import translate.complexity.clazz.LCOMNEvaluator;
import translate.complexity.clazz.NOCEvaluator;
import translate.complexity.clazz.RFCEvaluator;
import translate.complexity.clazz.WMCBaseEvaluator;
import translate.complexity.method.CyclomaticEvaluator;
import translate.component.MemberFormatter;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    interface System {
        ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<Node> allClazz, MemberFormatter formatter);
    }

    interface Clazz {
        ComplexityEvaluator.Clazz[] EVALUATORS = {
            new DITEvaluator(),
            new NOCEvaluator(),
            new WMCBaseEvaluator("WMC/unity", 20,  -1, (a, b, f) -> ComplexityMetricResult.builder().value(1).build()),
            //new CBOEvaluator(),
            new RFCEvaluator(),
            new LCOMNEvaluator()
        };

        ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz, MemberFormatter formatter);
    }

    interface Method {
        ComplexityEvaluator.Method[] EVALUATORS = {
            new CyclomaticEvaluator()
        };

        ComplexityMetricResult calculate(Node clazz, MethodDeclaration method, MemberFormatter formatter);
    }
}
