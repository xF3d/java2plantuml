package visitors;

import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.Translator;

public class EnumVisitor extends VoidVisitorAdapter<Void> {

    private final Translator translator;

    public EnumVisitor(Translator translator){
        this.translator=translator;
    }

    @Override
    public void visit(EnumDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
