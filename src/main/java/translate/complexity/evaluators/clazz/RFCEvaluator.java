package translate.complexity.evaluators.clazz;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import translate.ResolverUtils;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;

import java.util.HashSet;
import java.util.Set;

/**
 * Counts the number of methods that can potentially execute in response to a message sent to an object (its own methods plus methods they invoke).
 * High RFC means the class has more possible behaviors, increasing complexity, testing effort, and the likelihood of defects.
 */
public class RFCEvaluator implements ComplexityEvaluator.Clazz {

    @Override
    public ComplexityMetricResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz) {
        ComplexityMetricResult.Builder builder = ComplexityMetricResult.builder()
                .name("RFC")
                .max(100)
                .min(-1);

        // this is for inherited methods aswell, but throws errors when parameter can't be found
        int nlm;
        try {
            ResolvedReferenceTypeDeclaration rtd = ResolverUtils.resolve(clazz, ResolvedReferenceTypeDeclaration.class);
            nlm = rtd.getAllMethods().size();
        } catch (Exception e) {
            nlm = clazz.getMethods().size();
        }

        String classPackageName = clazz.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(PackageDeclaration::getName)
                .map(Name::asString)
                .orElse("") + MemberFormatter.fullSimpleName(clazz);

        Set<String> resolvedMethods = new HashSet<>(); // to avoid double counting
        for (MethodDeclaration method : clazz.getMethods()) {
            method.findAll(MethodCallExpr.class).forEach(call -> {
                try {
                    ResolvedMethodDeclaration resolved = call.resolve();
                    if(resolved.getPackageName().equals(classPackageName)) return;
                    resolvedMethods.add(resolved.getQualifiedSignature()); // className.method(args)
                } catch (Exception e) {
                    //System.out.println(call.getNameAsExpression());
                    resolvedMethods.add("EXTERNAL." + call.getNameAsString() + "." + call.getArguments().size());
                }
            });
        }

        int nrm = resolvedMethods.size();

        return builder.value(nlm + nrm).build();
    }

    @Override
    public String getName() {
        return "RFC";
    }
}
