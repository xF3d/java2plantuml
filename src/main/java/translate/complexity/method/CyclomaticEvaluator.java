package translate.complexity.method;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;
import translate.component.formatter.MemberFormatter;

public class CyclomaticEvaluator implements ComplexityEvaluator.Method {

    @Override
    public ComplexityMetricResult calculate(TypeDeclaration<?> clazz, MethodDeclaration method, MemberFormatter formatter) {
        // Fix 1: Localize builder initialization to guarantee thread safety
        var builder = ComplexityMetricResult.builder()
                .name("Cyclomatic")
                .max(20)
                .min(-1);

        var optBody = method.getBody();
        if (optBody.isEmpty()) {
            return builder.value(1).build();
        }

        // Base complexity for any executable method is 1
        CyclomaticVisitor visitor = new CyclomaticVisitor();
        optBody.get().accept(visitor, null);

        int complexity = visitor.getComplexityScore() + 1;
        return builder.value(complexity).build();
    }

    @Override
    public String getName() {
        return "Cyclomatic";
    }

    /**
     * Inner visitor class to robustly isolate and count branching paths.
     */
    private static class CyclomaticVisitor extends VoidVisitorAdapter<Void> {
        private int complexityScore = 0;

        public int getComplexityScore() {
            return complexityScore;
        }

        @Override
        public void visit(IfStmt n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(WhileStmt n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(DoStmt n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(ForStmt n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(ForEachStmt n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            complexityScore++;
            super.visit(n, arg);
        }

        @Override
        public void visit(SwitchEntry n, Void arg) {
            // Cyclomatic complexity increments for specific case paths, excluding default
            if (n.getLabels().isNonEmpty()) {
                complexityScore++;
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(ConditionalExpr n, Void arg) {
            complexityScore++; // Ternary operators count as a branch path
            super.visit(n, arg);
        }

        @Override
        public void visit(BinaryExpr n, Void arg) {
            // Logical AND/OR expressions split paths
            if (n.getOperator() == BinaryExpr.Operator.AND || n.getOperator() == BinaryExpr.Operator.OR) {
                complexityScore++;
            }
            super.visit(n, arg);
        }
    }
}
