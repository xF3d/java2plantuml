package visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.AllArgsConstructor;
import translate.translator.NodeHandler;

@AllArgsConstructor
public class ClassVisitor extends VoidVisitorAdapter<Void> {

    private final NodeHandler translator;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        if (!n.isInterface()) {
            this.translator.addNode(n);
        }

        super.visit(n, arg);
    }
}
