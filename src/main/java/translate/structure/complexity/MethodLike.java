package translate.structure.complexity;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.Optional;

public interface MethodLike<T extends CallableDeclaration<T>> {
    Optional<BlockStmt> body();

    T raw();

    record Method(MethodDeclaration raw) implements MethodLike<MethodDeclaration> {
        @Override
        public Optional<BlockStmt> body() {
            return raw.getBody();
        }
    }

    record Constructor(ConstructorDeclaration raw) implements MethodLike<ConstructorDeclaration> {
        @Override
        public Optional<BlockStmt> body() {
            return Optional.of(raw.getBody());
        }
    }

    static MethodLike<?> of(CallableDeclaration<?> raw) {
        if (raw instanceof MethodDeclaration rawM) return new Method(rawM);
        if (raw instanceof ConstructorDeclaration rawC) return new Constructor(rawC);

        throw new IllegalArgumentException("Invalid type of MethodLike for " + raw);
    }
}
