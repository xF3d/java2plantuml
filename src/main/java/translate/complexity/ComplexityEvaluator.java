package translate.complexity;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import translate.complexity.clazz.DITEvaluator;
import translate.complexity.clazz.LCOMNEvaluator;
import translate.complexity.clazz.NOCEvaluator;
import translate.complexity.clazz.RFCEvaluator;
import translate.complexity.clazz.WMCBaseEvaluator;
import translate.complexity.method.CyclomaticEvaluator;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import java.util.Collection;

public interface ComplexityEvaluator {
    String getName();

    interface System extends ComplexityEvaluator {
        ComplexityEvaluator.System[] EVALUATORS = {
        };

        ComplexityMetricResult calculate(ComplexityTranslator translator, Collection<TypeDeclaration<?>> allClazz, MemberFormatter formatter);
    }

    interface Clazz extends ComplexityEvaluator {
        ComplexityEvaluator.Clazz[] EVALUATORS = {
            new DITEvaluator(),
            new NOCEvaluator(),
            new WMCBaseEvaluator("WMC/unity", 20, -1) {
                @Override
                public ComplexityMetricResult evaluateMethod(Node clazz, MethodDeclaration method, MemberFormatter formatter) {
                    return ComplexityMetricResult.builder().value(1).build();
                }
            },
            //new CBOEvaluator(),
            new RFCEvaluator(),
            new LCOMNEvaluator()
        };

        ComplexityMetricResult calculate(Collection<TypeDeclaration<?>> allClazz, Node clazz, MemberFormatter formatter);
    }

    interface Method extends ComplexityEvaluator {
        ComplexityEvaluator.Method[] EVALUATORS = {
            new CyclomaticEvaluator()
        };

        ComplexityMetricResult calculate(TypeDeclaration<?> clazz, MethodDeclaration method, MemberFormatter formatter);
    }
}
