package translate.structure.complexity;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.ComplexityResultAggregator;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class ComplexityMethod implements FutureComplexityResultHolder {
    private final MethodLike<?> method;
    private final CompletableFuture<ComplexityResultAggregator> complexityResult;

    public boolean isConstructor() {
        return method instanceof MethodLike.Constructor;
    }

    public boolean isMethod() {
        return method instanceof MethodLike.Method;
    }
}
