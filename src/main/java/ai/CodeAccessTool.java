package ai;

import dev.langchain4j.agent.tool.*;
import file.FileLogger;
import lombok.AllArgsConstructor;
import translate.translator.SimpleTranslator;
import translate.translator.exceptions.InvalidSelectedClassException;

@AllArgsConstructor
public class CodeAccessTool {
    private final SimpleTranslator translator;

    @Tool("getMethodBody: Obtain method code of a method or constructor defined in a specified class or an error if failed")
    public String getMethodBody(
            @P("Fully Qualified Name of the target class from where you need the method") String clazz,
            @P("Target method's signature; for constructors instead of method name use the class's name") String methodSignature
    ) {
        FileLogger.LOGGER.info("getMethodBody tool call " + clazz + "#" + methodSignature + " " + translator);
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

    @Tool("doesClassExist: Obtain whatever or not a class exists with a specified FQN, can return TRUE, FALSE, ERROR")
    public String doesClassExist(@P("Fully Qualified Name of the target class") String clazz) {
        FileLogger.LOGGER.info("doesClassExist tool call: " + clazz + ", " + translator);
        if (clazz == null) return "ERROR";

        String realFqn = translator.getPackageManager().mapRaw(clazz);
        if (realFqn == null) return "FALSE";

        try {
            translator.getTypeDeclaration(realFqn);
            return "TRUE";
        } catch (InvalidSelectedClassException e) {
            return "FALSE";
        }
    }
}
