package translate.translator;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.Getter;
import translate.component.formatter.MemberFormatter;
import translate.structure.pkg.PackageManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SimpleTranslator implements Translator {
    private final PackageManager packageManager = new PackageManager();
    private final MemberFormatter formatter = new MemberFormatter(packageManager);

    private final Map<String, TypeDeclaration<?>> classMap = new HashMap<>();
    private final Map<String, Map<String, MethodDeclaration>> methodMap = new HashMap<>();
    private boolean executed;

    @Override
    public void addNode(Node node) {
        if (!(node instanceof TypeDeclaration<?> decl)) throw new IllegalArgumentException("Should never happen");

        String fullName = formatter.fullPackageName(node.findCompilationUnit().orElseThrow(), node);
        classMap.put(fullName, decl);

        //TODO: this doesn't include inherited methods, are they really necessary tho?
        Map<String, MethodDeclaration> methods = methodMap.get(fullName);
        if (methods == null) {
            methods = new HashMap<>();
        }

        for (MethodDeclaration method : decl.getMethods()) {
            methods.put(method.getSignature().asString(), method);
        }

        methodMap.put(fullName, methods);
    }

    @Override
    public void setError(Boolean b) {

    }

    @Override
    public boolean execute(File selectedDirectory) {
        boolean result = Translator.super.execute(selectedDirectory);
        executed = true;
        return result;
    }

    @Override
    public PackageManager getPackageManager() {
        return packageManager;
    }

    @Override
    public boolean hasExecuted() {
        return executed;
    }
}
