package visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.translator.Translator;

public class InterfaceVisitor extends VoidVisitorAdapter<Void> {

    private final Translator translator;

    public InterfaceVisitor(Translator translator){
        this.translator=translator;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {

        translator.addNode(n);
        super.visit(n, arg);

    }
}
