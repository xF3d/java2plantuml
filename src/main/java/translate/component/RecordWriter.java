package translate.component;

import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import translate.ClassDiagramConfig;

public class RecordWriter extends SetTranslatingComponent<RecordDeclaration> {
    protected RecordWriter(ClassDiagramConfig cfg) {
        super(RecordDeclaration.class, cfg);
    }

    @Override
    public void writeComponent(RecordDeclaration element, StringBuilder builder) {
        builder.append("class ");
        builder.append(element.getName());
        builder.append("<<record>>");
        builder.append("{");
        builder.append("\n");

        //attributes
        if (config.isShowAttributes()) {
            writeAttributes(r, builder);
        }

        if (config.isShowMethods()) {
            //methods
            writeConstructors(r, builder);
            writeMethods(r, builder);
        }

        builder.append("}\n");

        //implemented interfaces
        for (ClassOrInterfaceType e : element.getImplementedTypes()) {

            builder.append(e.getName());
            builder.append(" --|> ");
            builder.append(e.getName());
            builder.append("\n");
        }
    }
}
