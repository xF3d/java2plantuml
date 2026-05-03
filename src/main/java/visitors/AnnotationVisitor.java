package visitors;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.translator.Translator;

public class AnnotationVisitor extends VoidVisitorAdapter<Void> {
    private final Translator translator;

    public AnnotationVisitor(Translator translator){
        this.translator = translator;
    }

    @Override
    public void visit(AnnotationDeclaration n, Void arg) {
        this.translator.addNode(n);
        super.visit(n, arg);
    }
}
