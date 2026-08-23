package visitors;

import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.translator.NodeHandler;

@AllArgsConstructor
public class RecordVisitor extends VoidVisitorAdapter<Void> {
    private final NodeHandler translator;

    @Override
    public void visit(RecordDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
