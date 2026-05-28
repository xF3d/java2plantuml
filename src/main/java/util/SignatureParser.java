package util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignatureParser {
    // Matches:
    // methodName(param : Type, other : Type) : ReturnType
    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*(\\w+)\\s*\\((.*)\\)\\s*:\\s*(.+?)\\s*$");

    public static String extractSignature(String input) {
        int index = input.lastIndexOf('}');

        // remove modifiers
        if (index != -1) {
            input = input.substring(index + 1);
        }

        Matcher matcher = METHOD_PATTERN.matcher(input);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid method format: " + input);
        }

        String methodName = matcher.group(1);
        String params = matcher.group(2).trim();

        if (params.isEmpty()) {
            return methodName + "()";
        }

        List<String> paramList = splitParameters(params);

        StringBuilder signature = new StringBuilder();
        signature.append(methodName).append("(");

        for (int i = 0; i < paramList.size(); i++) {
            String param = paramList.get(i);

            // Split "name : Type"
            String[] parts = param.split("\\s*:\\s*", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid parameter: " + param);
            }

            String type = parts[1].trim();

            signature.append(type);

            if (i < paramList.size() - 1) {
                signature.append(", ");
            }
        }

        signature.append(")");

        return signature.toString();
    }

    /**
     * Splits parameters while respecting removing generic types.
     */
    private static List<String> splitParameters(String params) {
        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        int genericDepth = 0;

        for (char c : params.toCharArray()) {
            switch (c) {
                case '<':
                    genericDepth++;
                    break;

                case '>':
                    genericDepth--;
                    break;

                case ',':
                    if (genericDepth == 0) {
                        result.add(current.toString().trim());
                        current.setLength(0);
                    }
                    break;

                default:
                    if (genericDepth == 0)
                        current.append(c);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        return result;
    }


    public static boolean isConstructor(String input) {
        char[] charArray = input.toCharArray();
        for (int i = input.length() - 1; i > 0; i--) {
            char selected = charArray[i];
            if (selected == ')') return true;
            if (selected == ':') return false;
        }

        throw new IllegalArgumentException("Invalid method to begin with");
    }
}
