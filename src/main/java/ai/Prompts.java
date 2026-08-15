package ai;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import lombok.experimental.UtilityClass;
import translate.structure.complexity.ComplexityClass;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@UtilityClass
public class Prompts {
    // TODO: elemento centrale durante la presentazione: prompts
    // TODO: aggiunta riepilogo tecniche di refactoring usate e dove
    // TODO: mostrare metodo originale nel DiffPanel
    // TODO: dove mettere test?
    // TODO: class context
    public static final String FORMAT_USAGE_REFACTOR_METHOD = """
            Your answer should always follow this strict format, where text wrapped by %% are place holders.
            
            -----METHODS-----
            %%New methods, MUST have the rewritten method with the same signature%%
            -----NOTES-----
            %%AI_MODEL_NAME%% : %%NOTE_1%%
            %%AI_MODEL_NAME%% : %%NOTE_2%%
            %%AI_MODEL_NAME_2%% : %%NOTE_3%%
            
            
            
            All %%Note_n%% must be maximum of 50 words and contain suggestions, not comments.
            Must be Java 17 language level max.
            
            
            Example (DO NOT USE IN CONTEXT OR AS A FIRST PROMPT):
            -----METHODS-----
            public void myMethod(int a, int b) {
                this.c = a + b;
            }
            
            private void newMethod() {
                System.out.println("Inside new method");
            }
            -----NOTES-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            
            
            
            Don't use a preamble or an after message, just give me the answer in said format, nothing more, nothing less.
            The rewritten method should maintain the same signature as the previous one.
            """;

    public static final String FORMAT_USAGE_REFACTOR_CLASS = """
            Your answer should always follow this strict format, where text wrapped by %% are place holders.
            
            -----ADDED_IMPORTS-----
            %%Added imports of added classes or others, as long as they are used%%
            -----CHANGES-----
            %%Added fields, fields should not be removed%%
            %%Added methods%%
            %%Rewritten methods with the same signature, methods can't be removed but can be rewritten%%
            -----ADDED-----
            %%File contents with class, imports, package definition%%
            ===============
            %%File contents with class, imports, package definition n°2%%
            ...
            -----NOTES-----
            %%AI_MODEL_NAME%% : %%NOTE_1%%
            %%AI_MODEL_NAME%% : %%NOTE_2%%
            %%AI_MODEL_NAME_2%% : %%NOTE_3%%
            
            
            
            All %%Note_n%% must be maximum of 50 words and contain suggestions, not comments.
            Must be Java 25 language level max.
            Changes should only contain method declarations and field declarations, so no imports or type declarations
            
            
            Example (DO NOT USE IN CONTEXT OR AS A FIRST PROMPT):
            -----ADDED_IMPORTS-----
            import org.my.otherpackage.Calculator;
            import org.my.package.MyPrinter;
            -----CHANGES-----
            private int c;
            
            public void myMethod(int a, int b) {
                this.c = new Calculator(a, b).add();
            }
            
            private void newMethod() {
                MyPrinter.print("Inside new method);
            }
            -----ADDED-----
            package org.my.package;
            
            public class MyPrinter {
                public static void print(String toprint) {
                    System.out.println(toprint);
                }
            }
            ===============
            package org.my.otherpackage;

            public record Calculator(int x, int y) {
                public int add() {
                    return x + y;
                }
            }
            -----NOTES-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            
            
            The example is in a very dumbed down format, in the real response use meaningful package, variable, classes names and method signatures.
            
            Don't use a preamble or an after message, just give me the answer in said format, nothing more, nothing less.
            """;

    public static final String SYSTEM_REFACTOR_METHOD_VER = """
            You are an AI agent named "%s" that given java code and a proposed new implementation and new methods you need to verify and improve the solution only when possible.
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            - Previous response
            
            """
            + FORMAT_USAGE_REFACTOR_METHOD +
            """
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself. That means that blueprints, or anything not fully implemented should not be added
            - NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            - If you don't want to change anything, just resend the message you received without changing it
            - You can add notes with your name only
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            
            You can change the METHODS as you see fit if necessary, but not the NOTE section, you can only add to the notes, not delete them
            """;

    public static final String SYSTEM_REFACTOR_METHOD_GEN = """
            You are an AI agent named "%s-GEN" that given java source code with some problematic metrics and/or code smells,
            has the job to fix said issues.
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            
            """
            + FORMAT_USAGE_REFACTOR_METHOD +
            """
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself. That means that blueprints, or anything not fully implemented should not be added
            - NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            - Only answer in the format provided
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            """;

    public static final String SYSTEM_REFACTOR_CLASS_VER = """
            You are an AI agent named "%s" that given a class's info, some tools,
            given new implementations of methods and new methods you need to verify and improve the solution only when possible.
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            - Previous response
            
            """
            + FORMAT_USAGE_REFACTOR_CLASS +
            """
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself. That means that blueprints, or anything not fully implemented or with only a comment should not be added
            - NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            - If you don't want to change anything, just resend the message you received without changing it
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Adding an external dependency
            - Modify another existing class other than the target ones, you can add as many as you want however in the ADDED section
            
            You can change the CHANGES and ADDED section as you see fit if necessary, but not the NOTE section, you can only add to the notes, not delete them
            """;

    public static final String SYSTEM_REFACTOR_CLASS_GEN = """
            You are an AI agent named "%s-GEN" that given a class's info, some tools,
            with some problematic metrics and/or code smells, has the job to fix said issues.
            
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            
            """
            + FORMAT_USAGE_REFACTOR_CLASS +
            """
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself. That means that blueprints, or anything not fully implemented or with only a comment should not be added
            - NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            - Only answer in the format provided
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Adding an external dependency
            - Modify another existing class other than the target ones, you can add as many as you want however in the ADDED section
            """;
    

    public static final String CLASS_CONTEXT = """
            IMPORTS
            %s
            
            SELECTED_CLASS
            %s
            
            FIELDS_IN_CLASS
            %s
            
            METHODS_IN_CLASS
            %s
            
            CLASS_METRICS
            %s
            
            CLASS_CODE_SMELLS
            %s
            
            """;

    public static final String METHOD_CONTEXT = """
            METHOD_METRICS
            %s
            
            METHOD_CODE_SMELLS
            %s
            
            ORIGINAL_SELECTED_METHOD
            %s
            
            """;


    public static String formattedClassContext(ComplexityClass clazz, boolean metricsForEach) {
        CompilationUnit cu = clazz.getNode().findCompilationUnit().orElseThrow();
        ArrayList<String> imports = new ArrayList<>(cu.getImports().size());
        for (ImportDeclaration singleImport : cu.getImports()) {
            String importString = singleImport.getNameAsString();
            if (singleImport.isStatic()) {
                importString = "static " + importString;
            }

            if (singleImport.isAsterisk()) {
                importString += ".*";
            }

            imports.add(importString);
        }

        String importString = String.join("\n", imports);

        var decl = clazz.getNode();
        ArrayList<String> methodSignatures = new ArrayList<>(decl.getMethods().size());
        for (var methodDecl : decl.getMethods()) {
            String signature = methodDecl.getSignature().asString();

            if (metricsForEach) {
                methodSignatures.add(signature + " Metrics --> " + clazz.getMethods()
                        .get(signature)
                        .evalResult()
                        .metricsAi("; ")
                );
            } else {
                methodSignatures.add(signature);
            }
        }

        for (var ctorDecl : decl.getConstructors()) {
            String signature = ctorDecl.getSignature().asString();

            if (metricsForEach) {
                methodSignatures.add(signature + " Metrics --> " + clazz.getMethods()
                        .get(signature)
                        .evalResult()
                        .metricsAi("; ")
                );
            } else {
                methodSignatures.add(signature);
            }
        }

        String fieldStrings = decl.getFields().stream()
                .map(FieldDeclaration::toString)
                .collect(Collectors.joining("\n"));
        String methodSignaturesString = String.join("\n", methodSignatures);

        String classMetrics;
        String classSmells;
        try {

            var clzResult = clazz.getComplexityResult().get();
            classMetrics = clzResult.metricsAi();
            classSmells = clzResult.codeSmellsAi();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return Prompts.CLASS_CONTEXT.formatted(
                importString,
                decl.getNameAsString(),
                fieldStrings,
                methodSignaturesString,
                classMetrics,
                classSmells
        );
    }
}
