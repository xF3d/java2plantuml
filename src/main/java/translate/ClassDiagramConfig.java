package translate;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class ClassDiagramConfig {

    private List<VoidVisitorAdapter<Void>> visitorAdapters;
    private boolean showMethods;
    private boolean showAttributes;
    private boolean showColoredAccessSpecifiers;
    private boolean showAssociations;

    //builder
    @Setter
    @Accessors(chain = true)
    public static class Builder implements UmlConfigBuilder {

        //default values for attributes
        private final List<VoidVisitorAdapter<Void>> visitorAdapters = new ArrayList<>();
        private boolean showMethods = true;
        private boolean showAttributes = true;
        private boolean showColoredAccessSpecifiers = false;
        private boolean showAssociations;

        public Builder withVisitor(VoidVisitorAdapter<Void> visitor) {
            visitorAdapters.add(visitor);
            return this;
        }

        @Override
        public ClassDiagramConfig build() {

            ClassDiagramConfig config = new ClassDiagramConfig();
            config.setVisitorAdapters(visitorAdapters);
            config.setShowAttributes(showAttributes);
            config.setShowColoredAccessSpecifiers(showColoredAccessSpecifiers);
            config.setShowMethods(showMethods);
            config.setShowAssociations(showAssociations);
            return config;
        }
    }
}
