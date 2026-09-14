package translate.complexity.evaluators.method;

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
import lombok.Getter;
import translate.complexity.MetricDefinition;
import translate.complexity.MetricDefinitionRegistry;
import translate.complexity.evaluators.ComplexityEvaluator;
import translate.complexity.result.BoundedResult;
import translate.structure.complexity.MethodLike;
import translate.translator.ComplexityTranslator;

public class CyclomaticEvaluator implements ComplexityEvaluator.Method {

    @Override
    public BoundedResult calculate(ComplexityTranslator translator, TypeDeclaration<?> clazz, MethodLike<?> method) {
        var optBody = method.body();
        if (optBody.isEmpty()) {
            return result(1);
        }

        CyclomaticVisitor visitor = new CyclomaticVisitor();
        optBody.get().accept(visitor, null);

        int complexity = visitor.getComplexityScore() + 1;
        return result(complexity);
    }

    @Getter
    private static class CyclomaticVisitor extends VoidVisitorAdapter<Void> {
        private int complexityScore = 0;

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

    @Override
    public MetricDefinition getDefinition() {
        return MetricDefinitionRegistry.get("Cyclomatic");
    }
}
