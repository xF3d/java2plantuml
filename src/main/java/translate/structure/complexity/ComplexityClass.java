package translate.structure.complexity;

import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.ComplexityResultAggregator;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class ComplexityClass {
    private final TypeDeclaration<?> node;
    private final CompletableFuture<ComplexityResultAggregator> complexityResult;
}
