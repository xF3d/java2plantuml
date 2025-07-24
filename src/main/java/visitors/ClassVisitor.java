package visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.translator.Translator;

public class ClassVisitor extends VoidVisitorAdapter<Void> {

    private final Translator translator;

    public ClassVisitor(Translator translator){
        this.translator=translator;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
