package ai;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import lombok.experimental.UtilityClass;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@UtilityClass
public class Prompts {
    // TODO: elemento centrale durante la presentazione: prompts
    // TODO: dove mettere test?
    public static final String FORMAT_USAGE_REFACTOR_METHOD = """
            Your answer should always follow this strict format, where text wrapped by %% are place holders.
            
            -----METHODS-----
            %%New methods, MUST have the rewritten method with the same signature%%
            -----EXPLANATION-----
            %%Explanation of the changes/additions to the original class, max 100 words in a single line%%
            -----NOTES-----
            %%Different or no NOTES%%
            
            Example (DO NOT USE IN CONTEXT OR AS A FIRST PROMPT):
            -----METHODS-----
            public void myMethod(int a, int b) {
                this.c = a + b;
            }
            
            private void newMethod() {
                System.out.println("Inside new method");
            }
            -----EXPLANATION-----
            Simplified logic
            -----NOTES-----
            myCalc's class might be a overabstraction, consider removing them
            
            All %%NOTE_n%% must be maximum of 50 words and contain suggestions, not comments.
            Must be Java 25 language level max.
            Don't use a preamble or an after message, just give me the answer in said format, nothing more, nothing less.
            The rewritten method should maintain the same signature as the previous one.
            All methods created or modified should contain the full implementation if they are not abstract.
            """;

    public static final String FORMAT_USAGE_REFACTOR_CLASS = """
            Your answer should always follow this strict format.
            
            -----ADDED_IMPORTS-----
            <added-imports>
            -----CHANGES-----
            <added-fields>
            <added-methods>
            <rewritten-methods>
            -----ADDED-----
            <class-file-x>
            ===============
            <class-file-x>
            -----EXPLANATION-----
            <explanation>
            -----NOTES-----
            <note-x>
            <note-x>
            
            Where you replace the placeholders:
            <added-imports>: with added imports to the SELECTED_CLASS
            <added-fields>: with added fields to the SELECTED_CLASS
            <added-methods>: with added methods to the SELECTED_CLASS
            <rewritten-methods>: with rewritten methods, with the same signature as the past method, to swap out in SELECTED_CLASS
            <class-file-x>: one or more classes you want to add, the file will be added at the defined package declaration and must contain a type definition and imports, 
            useful if you want to extract some code or functionality to another class. The added class must not already exist somewhere else.
            <explanation>: Explanation of the changes/additions to the original class, max 100 words in a single line
            <note-x>: one or more notes containing suggestion for the user that you can't do directly within this format, for instance removing methods and fields isn't allowed
            
            Example answer after getting sent class context:
            
            -----ADDED_IMPORTS-----
            import org.my.package;
            import org.my.otherpackage;
            -----CHANGES-----
            public void rewrittenMethod() {
                newMethod();
                oldMethod();
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
            -----EXPLANATION-----
            Extracted functionality to new classes and methods
            -----NOTES-----
            
            
            All notes must be maximum of 50 words and contain suggestions, not comments.
            Must be Java 25 language level max.
            Changes should only contain method declarations and field declarations, so no imports or type declarations
            Everything wrapped in a <> are placeholders, and as such should not be inserted in the answer.
            All methods created or modified should contain the full implementation if they are not abstract, stub methods with only comments.
            
            The example used is to give you a better understanding of the format only, don't reuse the names and use proper naming in real output
            Don't use a preamble or an after message, just give me the answer in said format, nothing more, nothing less.
            You cannot edit any other class other than the SELECTED_CLASS.
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
            If you don't want to change anything, just send UNCHANGED only without this format,
            
            You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            
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
            You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            Only answer in the format provided
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            """;

    public static final String SYSTEM_REFACTOR_CLASS_VER = """
            You are an AI agent named "%s" that given a class's info,
            given new implementations of methods and new methods you need to verify and improve the solution only when possible.
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            - Previous response
            
            You should focus your getMethodBody calls on the SELECTED_CLASS, especially the problematic or long methods
            Use the doesClassExist calls mainly to double check that a class doesn't exist already.
            
            """
            + FORMAT_USAGE_REFACTOR_CLASS +
            """
            If you don't want to change anything, just send UNCHANGED only without this format.
            
            You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Adding an external dependency
            - Modify another existing class other than the target ones, you can add as many as you want however in the ADDED section
            
            You can change the CHANGES and ADDED section as you see fit if necessary, but not the NOTE section, you can only add to the notes, not delete them
            """;

    public static final String SYSTEM_REFACTOR_CLASS_GEN = """
            You are an AI agent named "%s-GEN" that given a class's info,
            with some problematic metrics and/or code smells, has the job to fix said issues.
            
            You should focus your getMethodBody calls on the SELECTED_CLASS, especially the problematic or long methods
            Use the doesClassExist calls mainly to double check that a class doesn't exist already.
            
            You will be also given relevant context like:
            - Metrics (which themselves don't necessarily indicate problems).
            - CodeSmells (which are indeed problems that need to be addressed).
            
            """
            + FORMAT_USAGE_REFACTOR_CLASS +
            """
            You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            NOTEs are optional, must be with your name, and should never contain explanations of your actions, they merely talk about stuff you can't fix directly and tips for the user
            Only answer in the format provided
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Adding an external dependency
            - Modify another existing class other than the target ones, you can add as many as you want however in the ADDED section
            """;
    

    public static final String METHOD_CONTEXT = """
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
            
            METHOD_METRICS
            %s
            
            METHOD_CODE_SMELLS
            %s
            
            ORIGINAL_SELECTED_METHOD
            %s
            
            """;

    public static final String CLASS_CONTEXT = """
            SELECTED_CLASS
            %s
            
            METHODS_IN_CLASS
            %s
            
            CLASS_METRICS
            %s
            
            CLASS_CODE_SMELLS
            %s
            
            """;

    public static String formattedMethodContext(ComplexityClass clazz, ComplexityMethod method) {
        CompilationUnit cu = clazz.getNode().findCompilationUnit().orElseThrow();
        ArrayList<String> imports = getImportStrings(cu);


        String importString = String.join("\n", imports);

        var decl = clazz.getNode();
        ArrayList<String> methodSignatures = new ArrayList<>(decl.getMethods().size());
        for (var methodDecl : decl.getMethods()) {
            String signature = methodDecl.getSignature().asString();

            methodSignatures.add(signature);
        }

        for (var ctorDecl : decl.getConstructors()) {
            String signature = ctorDecl.getSignature().asString();

            methodSignatures.add(signature);
        }

        String fieldStrings = decl.getFields().stream()
                .map(FieldDeclaration::toString)
                .collect(Collectors.joining("\n"));
        if (decl instanceof RecordDeclaration recordDeclaration) {
            fieldStrings += recordDeclaration.getParameters().stream()
                    .map(Parameter::getName)
                    .map(SimpleName::asString)
                    .map(it -> "record property " + it)
                    .collect(Collectors.joining("\n"));
        }

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

        String methodMetrics;
        String methodSmells;
        try {
            var mthResult = method.getComplexityResult().get();
            methodMetrics = mthResult.metricsAi();
            methodSmells = mthResult.codeSmellsAi();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return Prompts.METHOD_CONTEXT.formatted(
                importString,
                decl.getNameAsString() + " : " + decl.getClass().getSimpleName(),
                fieldStrings,
                methodSignaturesString,
                classMetrics,
                classSmells,
                methodMetrics,
                methodSmells,
                method.getMethod().toString()
        );
    }

    private static ArrayList<String> getImportStrings(CompilationUnit cu) {
        ArrayList<String> imports = new ArrayList<>(cu.getImports().size() + 1);

        cu.getPackageDeclaration().ifPresent((p) -> {
            imports.add("package " + p.getNameAsString() + ";");
        });

        for (ImportDeclaration singleImport : cu.getImports()) {
            String importString = singleImport.toString();
            if (singleImport.isStatic()) {
                importString = "import static " + importString;
                imports.add(importString);
                continue;
            }

            if (singleImport.isAsterisk()) {
                importString += ".*";
            }

            imports.add("import " + importString);
        }

        return imports;
    }

    public static String formattedClassContext(ComplexityClass clazz) {
        CompilationUnit cu = clazz.getNode().findCompilationUnit().orElseThrow();

        var decl = clazz.getNode();
        ArrayList<String> methodSignatures = new ArrayList<>(decl.getMethods().size());
        for (var methodDecl : decl.getMethods()) {
            String signature = methodDecl.getSignature().asString();

            methodSignatures.add(signature + " Metrics --> " + clazz.getMethods()
                    .get(signature)
                    .evalResult()
                    .metricsAi("; ")
            );
        }

        for (var ctorDecl : decl.getConstructors()) {
            String signature = ctorDecl.getSignature().asString();

            methodSignatures.add(signature + " Metrics --> " + clazz.getMethods()
                    .get(signature)
                    .evalResult()
                    .metricsAi("; ")
            );
        }

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
                cu.toString(),
                methodSignaturesString,
                classMetrics,
                classSmells
        );
    }
}
