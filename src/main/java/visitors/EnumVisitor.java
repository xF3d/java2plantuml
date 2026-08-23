package visitors;

import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.translator.NodeHandler;

@AllArgsConstructor
public class EnumVisitor extends VoidVisitorAdapter<Void> {

    private final NodeHandler translator;

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
