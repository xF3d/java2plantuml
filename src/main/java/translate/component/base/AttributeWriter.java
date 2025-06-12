package translate.component.base;

import com.github.javaparser.ast.body.FieldDeclaration;
import translate.ClassDiagramConfig;
import translate.component.SetTranslatingComponent;

public class AttributeWriter extends SetTranslatingComponent<FieldDeclaration> {
    protected AttributeWriter(ClassDiagramConfig config) {
        super(FieldDeclaration.class, config);
    }

    @Override
    public void writeComponent(FieldDeclaration element, StringBuilder builder) {
        writeModifiers(element.getModifiers(), builder);
        builder.append(element.getVariables().get(0).getName());
        builder.append(" : ");
        builder.append(element.getVariables().get(0).getType().asString());
        builder.append("\n");
    }
}
