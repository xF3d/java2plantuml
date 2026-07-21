package translate.structure.complexity;

import com.github.javaparser.ast.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.complexity.ComplexityResultAggregator;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class ComplexityClass {
    private final Node node;
    private final CompletableFuture<ComplexityResultAggregator> complexityResult;
}
