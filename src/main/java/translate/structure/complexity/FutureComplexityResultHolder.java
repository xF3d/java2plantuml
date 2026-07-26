package translate.structure.complexity;

import translate.complexity.ComplexityResultAggregator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface FutureComplexityResultHolder {
    CompletableFuture<ComplexityResultAggregator> getComplexityResult();

    default ComplexityResultAggregator evalResult() {
        try {
            return getComplexityResult().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
