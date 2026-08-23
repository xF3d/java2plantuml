package visitors;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.translator.NodeHandler;

@AllArgsConstructor
public class AnnotationVisitor extends VoidVisitorAdapter<Void> {
    private final NodeHandler translator;

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
