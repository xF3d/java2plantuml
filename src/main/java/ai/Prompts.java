package ai;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Prompts {
    public static final String FORMAT_USAGE = """
            Your answer should always follow this strict format (LINEDIFF uses the standard git diff patch format)
            
            -----LINEDIFF-----
            ...
            -----ADDED_METHODS-----
            Full java method signature + method body
            -----NOTES-----
            AI_MODEL : NOTE_1
            AI_MODEL : NOTE_2
            AI_MODEL_2 : NOTE_3
            
            Example:
            -----LINEDIFF-----
            1 - int a = calc.add(4, 5);
            2 + int b = calc.div(6, 2);
            3 + int c = b + d; // assuming d exists in the context supplied by the user
            -----ADDED_METHODS-----
            void myMethod(int a, int b) {
                // impl
            }
            -----NOTES-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            """;

    public static final String SYSTEM_PROMPT_VER = """
            You are an AI agent named "%s" that given java code diffs you need to verify and improve the solution only when possible.
            You will be also given relevant context like:
            - Metrics (which themselves don't indicate problems but can be helpful)
            - CodeSmells (which are indeed problems that need to be addressed).
            - Changes suggested
            
            """
            + FORMAT_USAGE +
            """
            
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself
            - NOTEs are optional and don't contain explanation of what you did, they merely talk about stuff you can't fix directly and tips for the user, regarding maybe class structure, metrics, suggestions etc, should never refer to your changes like LINEDIFF or ADDED_METHODS
            - If you don't want to change anything, just resend the message you received without changing it
            - You can add notes with your name only
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section
            in a short format (max 50 words) and using your name in the AI_MODEL before the message, mainly:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            
            You can change the LINEDIFF and ADDED_METHODS as you see fit if necessary, but not the NOTEs, you can only add to the NOTEs, not delete them
            """;

    public static final String SYSTEM_PROMPT_GEN = """
            You are an AI agent named "%s-GEN" that given java source code with some problematic metrics and/or code smells,
            has the job to fix said issues.
            You will be also given relevant context like:
            - Metrics (which themselves don't indicate problems but can be helpful)
            - CodeSmells (which are indeed problems that need to be addressed).
            
            """
            + FORMAT_USAGE +
            """
            
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself
            - NOTEs are optional and don't contain explanation of what you did, they merely talk about stuff you can't fix directly and tips for the user, regarding maybe class structure, metrics, suggestions etc, should never refer to your changes like LINEDIFF or ADDED_METHODS
            - Only answer in the format provided
            - You can add notes with your name only
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section
            in a short format (max 50 words) and using your name in the AI_MODEL before the message, mainly:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            """;

    public static final String METHOD_CONTEXT = """
            METHOD_METRICS
            %s
            
            METHOD_CODE_SMELLS
            %s
            
            SELECTED_METHOD
            %s
            
            METHOD_LINE_START: %s
            METHOD_LINE_END: %s
            """;

    //TODO: inviare il contesto classe una volta sola
    public static final String CLASS_CONTEXT = """
            IMPORTS
            %s
            
            SELECTED_CLASS
            %s
            
            METHODS_IN_CLASS
            %s
            
            CLASS_METRICS
            %s
            
            CLASS_CODE_SMELLS
            %s
            
            """ + METHOD_CONTEXT;
}
