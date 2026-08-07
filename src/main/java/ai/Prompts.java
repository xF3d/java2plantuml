package ai;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Prompts {
    public static final String FORMAT_USAGE = """
            Your answer should always follow this strict format, where text wrapped by {{}} are place holders.
            
            -----METHODS-----
            {{New methods, MUST have the rewritten method with the same signature}}
            -----NOTES-----
            {{AI_MODEL_NAME}} : {{NOTE_1}}
            {{AI_MODEL_NAME}} : {{NOTE_2}}
            {{AI_MODEL_NAME_2}} : {{NOTE_3}}
            
            
            
            All {{Note_n}} must be maximum of 50 words and contain suggestions, not commments.
            {{AI_MODEL_NAME}} entries might contain other AI model names different than yours, that is because
            multiple AI are interacting with it, but you must only use your own name when adding notes.
            Must be Java 17 language level max.
            
            
            Example (DO NOT USE IN CONTEXT OR AS A FIRST PROMPT):
            -----METHODS-----
            public void myMethod(int a, int b) {
                {{Implementation}}
            }
            
            private void newMethod() {
                {{Another Implementation}}
            }
            -----NOTES-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            
            
            
            Don't use a preamble or an after message, just give me the answer in said format, nothing more, nothing less.
            The rewritten method should maintain the same signature as the previous one.
            """;

    public static final String SYSTEM_PROMPT_VER = """
            You are an AI agent named "%s" that given java code and a proposed new implementation and new methods you need to verify and improve the solution only when possible.
            You will be also given relevant context like:
            - Metrics (which themselves don't indicate problems but can be helpful)
            - CodeSmells (which are indeed problems that need to be addressed).
            - Previous response
            
            """
            + FORMAT_USAGE +
            """
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself, blueprints, or anything not fully implemented
            - NOTEs are optional
            - NOTEs should never contain explanations of yours or past actions, they merely talk about stuff you can't fix directly and tips for the user
            - If you don't want to change anything, just resend the message you received without changing it
            - You can add notes with your name only
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
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
            - You can create new functions only if you implement them fully yourself, blueprints, or anything not fully implemented
            - NOTEs are optional
            - NOTEs should never contain explanations of yours or past actions, they merely talk about stuff you can't fix directly and tips for the user
            - Only answer in the format provided
            - You can add notes with your name only
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            """;

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
            
            """;

    public static final String METHOD_CONTEXT = """
            METHOD_METRICS
            %s
            
            METHOD_CODE_SMELLS
            %s
            
            ORIGINAL_SELECTED_METHOD
            %s
            
            """;

}
