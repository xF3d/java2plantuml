package translate.translator;

import com.github.javaparser.ast.Node;

import java.io.File;
import java.io.FileNotFoundException;

public interface Translator {

    void addNode(Node node);
    void setError(Boolean b);
    void translateFile(File f) throws FileNotFoundException;

}