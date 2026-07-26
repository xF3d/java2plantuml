package translate.structure.complexity;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.ComplexityResultAggregator;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class ComplexityMethod implements FutureComplexityResultHolder {
    private final MethodDeclaration method;
    private final CompletableFuture<ComplexityResultAggregator> complexityResult;
}
