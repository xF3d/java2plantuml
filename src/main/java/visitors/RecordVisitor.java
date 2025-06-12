package visitors;

import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import translate.Translator;

public class RecordVisitor extends VoidVisitorAdapter<Void> {
    private Translator translator;

    public RecordVisitor(Translator translator){
        this.translator=translator;
    }

    @Override
    public void visit(RecordDeclaration n, Void arg) {
        this.translator.addRecord(n);
        super.visit(n, arg);
    }

}
