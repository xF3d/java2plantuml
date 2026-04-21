package translate.complexity.method;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import translate.complexity.ComplexityEvaluator;
import translate.complexity.ComplexityMetricResult;

public class CyclomaticEvaluator implements ComplexityEvaluator.Method {
    private static final ComplexityMetricResult.ComplexityMetricResultBuilder builder = ComplexityMetricResult.builder()
        .name("Cyclomatic")
        .max(20)
        .min(-1);


    @Override
    public ComplexityMetricResult calculate(Node clazz, MethodDeclaration method) {
        var optBody = method.getBody();
        if (optBody.isEmpty()) {
            return builder.value(1).build();
        }

        var body = optBody.get();
        int complexity = processStatement(body) + 1;

        return builder.value(complexity).build();
    }

    public int processStatement(Statement stmt) {
        int[] out = {0};

        stmt.ifIfStmt(ifStmt -> {
            out[0] += processStatement(ifStmt.getThenStmt()) + 1;
            out[0] += processExpression(ifStmt.getCondition());
            ifStmt.getElseStmt().ifPresent(elseStmt -> {
                out[0] += processStatement(elseStmt);
            });
        });

        stmt.ifSwitchStmt(switchStmt -> {
            out[0] += switchStmt.getEntries().size();
            for (SwitchEntry entry : switchStmt.getEntries()) {
                for (Statement entryStmt : entry.getStatements()) {
                    out[0] += processStatement(entryStmt);
                }
            }
        });

        stmt.ifForStmt(forStmt -> {
            forStmt.getCompare().ifPresent(cond -> {
                out[0] += processExpression(cond);
            });
            out[0] += processStatement(forStmt.getBody()) + 1;
        });

        stmt.ifForEachStmt(forEachStmt -> {
            out[0] += processStatement(forEachStmt.getBody()) + 1;
        });

        stmt.ifWhileStmt(whileStmt -> {
            out[0] += processExpression(whileStmt.getCondition());
            out[0] += processStatement(whileStmt.getBody()) + 1;
        });

        stmt.ifDoStmt(doStmt -> {
            out[0] += processExpression(doStmt.getCondition());
            out[0] += processStatement(doStmt.getBody()) + 1;
        });

        stmt.ifBlockStmt(blockStmt -> {
            for (Statement entryStmt : blockStmt.getStatements()) {
                out[0] += processStatement(entryStmt);
            }
        });

        stmt.ifTryStmt(tryStmt -> {
            out[0] += processStatement(tryStmt.getTryBlock());
            for (CatchClause catchClause : tryStmt.getCatchClauses()) {
                out[0] += processStatement(catchClause.getBody()) + 1;
            }
        });

        return out[0];
    }

    int processExpression(Expression expr) {
        int count = 0;

        if (expr instanceof BinaryExpr bin) {
            if (bin.getOperator() == BinaryExpr.Operator.AND ||
                bin.getOperator() == BinaryExpr.Operator.OR) {
                count++;
            }
            count += processExpression(bin.getLeft());
            count += processExpression(bin.getRight());
        }

        if (expr instanceof ConditionalExpr cond) {
            count++; // ternary
            count += processExpression(cond.getCondition());
            count += processExpression(cond.getThenExpr());
            count += processExpression(cond.getElseExpr());
        }

        return count;
    }
}
