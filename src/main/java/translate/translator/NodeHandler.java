package translate.translator;

import com.github.javaparser.ast.Node;

@FunctionalInterface
public interface NodeHandler {
    void addNode(Node node);
}