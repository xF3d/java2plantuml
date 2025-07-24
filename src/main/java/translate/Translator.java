package translate;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;

import java.io.File;
import java.io.FileNotFoundException;

public interface Translator{

    void addNode(Node node);
    void addClass(ClassOrInterfaceDeclaration c);
    void addEnum(EnumDeclaration c);
    void addInterface(ClassOrInterfaceDeclaration i);
    void addRecord(RecordDeclaration r);
    void setError(Boolean b);
    void translateFile(File f) throws FileNotFoundException;

}