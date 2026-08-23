package translate;

import com.github.javaparser.ast.visitor.VoidVisitor;
import translate.translator.NodeHandler;
import translate.translator.Translator;
import visitors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VisitorsBuilder {
    private final ArrayList<Function<NodeHandler, VoidVisitor<Void>>> visitors = new ArrayList<>();

    public VisitorsBuilder(boolean allowClasses, boolean allowInterfaces, boolean allowRecords, boolean allowEnums, boolean allowAnnotations) {
        if (allowClasses) {
            visitors.add(ClassVisitor::new);
        }

        if (allowInterfaces) {
            visitors.add(InterfaceVisitor::new);
        }

        if (allowRecords) {
            visitors.add(RecordVisitor::new);
        }

        if (allowEnums) {
            visitors.add(EnumVisitor::new);
        }

        if (allowAnnotations) {
            visitors.add(AnnotationVisitor::new);
        }
    }

    public List<VoidVisitor<Void>> createVisitors(NodeHandler translator) {
        return visitors.stream().map(it -> it.apply(translator)).toList();
    }
}
