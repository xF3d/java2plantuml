package translate.structure.complexity;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.ComplexityResultAggregator;
import translate.structure.pkg.PackageManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor
public class ComplexityClass implements FutureComplexityResultHolder {
    private final TypeDeclaration<?> node;
    private final CompletableFuture<ComplexityResultAggregator> complexityResult;
    private final ConcurrentHashMap<String, ComplexityMethod> methods = new ConcurrentHashMap<>();

    public void addMethod(ComplexityMethod method) {
        methods.put(
                method.getMethod().raw().getSignature().asString(),
                method
        );
    }

    public String customFqn() {
        return PackageManager.resolveTypeDeclaration(node);
    }
}
