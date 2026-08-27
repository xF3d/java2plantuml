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
            // sometimes model are stupid and send <init> regardless for constructor calls
            if (methodSignature.startsWith("<init>")) {
                String[] split = clazz.split("\\.");
                String className = split[split.length - 1];

                methodSignature = methodSignature.replace("<init>", className);
            }

            var methodDecl = translator.getMethodLike(realFqn, methodSignature);
            if (methodDecl == null) return "ERROR: Invalid method";

            return methodDecl.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Tool("getClassContents: Obtain class contents of a class")
    public String getClassContents(
            @P("Fully Qualified Name of the target class") String clazz
    ) {
        FileLogger.LOGGER.info("getClassContents tool call: " + clazz + ", " + translator);
        if (clazz == null) return "ERROR";

        String realFqn = translator.getPackageManager().mapRaw(clazz);
        if (realFqn == null) return "ERROR: Class not present in the project";

        try {
            return translator.getTypeDeclaration(realFqn).toString();
        } catch (InvalidSelectedClassException e) {
            return "ERROR: Class in the project but could not be obtained";
        }
    }


    @Tool("doesClassExist: Obtain whatever or not a class exists with a specified FQN, can return TRUE, FALSE, ERROR")
    public String doesClassExist(
            @P("Fully Qualified Name of the target class") String clazz
    ) {
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
