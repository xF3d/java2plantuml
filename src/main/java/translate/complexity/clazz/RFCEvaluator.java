package translate.complexity.clazz;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.MemberFormatter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RFCEvaluator implements ComplexityEvaluator.Clazz {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
            .name("RFC")
            .max(100)
            .min(-1);

    @Override
    public ComplexityMetricResult calculate(Collection<Node> allClazz, Node clazz, MemberFormatter formatter) {
        // in RFC inherited methods must also be calculated
        if (!(clazz instanceof NodeWithMembers<?> nwm)) {
            return null;
        }

        // this is for inherited methods aswell, but throws errors when parameter can't be found
        //ResolvedReferenceTypeDeclaration rtd = ComplexityUtils.resolve(clazz);
        //int nlm = rtd.getAllMethods().size();
        int nlm = nwm.getMethods().size();

        Set<String> resolvedMethods = new HashSet<>(); // to avoid double counting
        for (MethodDeclaration method : nwm.getMethods()) {
            method.findAll(MethodCallExpr.class).forEach(call -> {
                try {
                    ResolvedMethodDeclaration resolved = call.resolve();
                    resolvedMethods.add(resolved.getQualifiedSignature()); // className.method(args)
                } catch (Exception e) {
                    //System.out.println(call.getNameAsExpression());
                    resolvedMethods.add("EXTERNAL." + call.getNameAsExpression() + "." + call.getArguments().size());
                }
            });
        }

        int nrm = resolvedMethods.size();

        return builder.value(nlm + nrm).build();
    }
}
