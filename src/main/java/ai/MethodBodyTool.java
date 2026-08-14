package ai;

import dev.langchain4j.agent.tool.*;
import lombok.AllArgsConstructor;
import translate.translator.SimpleTranslator;

@AllArgsConstructor
public class MethodBodyTool {
    private final SimpleTranslator translator;

    @Tool("Obtain method code of a method or constructor defined in a specified class or an error if failed")
    public String getMethodBody(
            @P("FQN of the target class from where you need the method") String clazz,
            @P("Target method's signature; for constructors instead of method name use the class's name") String methodSignature
    ) {
        if (clazz == null || methodSignature == null) return "Invalid arguments";

        String realFqn = translator.getPackageManager().mapRaw(clazz);
        if (realFqn == null) return "ERROR: Invalid class";

        try {
            var methodDecl = translator.getMethodLike(realFqn, methodSignature);
            if (methodDecl == null) return "ERROR: Invalid method";

            return methodDecl.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
