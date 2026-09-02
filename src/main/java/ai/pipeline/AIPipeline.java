package ai.pipeline;

import ai.AIManager;
import ai.reponse.ApplicableDiff;
import ai.reponse.MessageValidator;
import ai.reponse.ValidatedResponse;
import ai.chat.BaseChat;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import file.Config;
import file.FileLogger;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AIPipeline {
    protected final String origin;
    protected final MessageValidator validator;
    protected final BaseChat suggestion;
    protected final List<BaseChat> verifiers;

    protected int outputTokenUsed = 0;
    protected int inputTokenUsed = 0;

    public AIPipeline(String origin, MessageValidator validator) {
        this.origin = origin;
        this.validator = validator;
        this.suggestion = initSuggestion();
        this.verifiers = initVerifiers();
    }

    protected abstract BaseChat initSuggestion();

    protected abstract List<BaseChat> initVerifiers();

    protected ValidatedResponse validatedChat(BaseChat model, String message) {
        ValidatedResponse lastResponse = null;
        for (int i = 0; i < Config.getInstance().getValidationRetries(); i++) {
            String stringResponse;
            try {
                Result<String> response = model.chat(TextContent.from(message));
                stringResponse = response.content();

                TokenUsage usage = response.tokenUsage();
                Integer in = usage.inputTokenCount();
                if (in != null) {
                    inputTokenUsed += in;
                }

                Integer out = usage.outputTokenCount();
                if (out != null) {
                    outputTokenUsed += out;
                }

                System.out.println(stringResponse);
            } catch (Exception e) {
                lastResponse = ValidatedResponse.error(e.getMessage());
                e.printStackTrace();
                continue;
            }

            ValidatedResponse validated = validator.validate(origin, stringResponse);
            if (validated.isValid()) {
                return validated;
            } else {
                lastResponse = validated;
            }
        }

        return lastResponse;
    }

    protected ValidatedResponse message(String mainMessage) {
        ValidatedResponse response = this.validatedChat(suggestion, mainMessage);

        if (response.isError()) {
            FileLogger.LOGGER.severe("Fatal error in generator: " + response.error());
            return ValidatedResponse.error(response.error());
        }

        var cfg = Config.getInstance();
        List<String> notes = new ArrayList<>(response.parsed().notes()
                .stream()
                .map(it -> cfg.getMainModel().modelName() + " : " + it)
                .toList()
        );

        System.out.println(response);
        ValidatedResponse previousResponse = response;

        var verifiersAi = AIManager.getInstance().getVerifiers();
        for (int i = 0; i < verifiers.size(); i++) {
            BaseChat verifier = verifiers.get(i);

            ValidatedResponse newResponse = this.validatedChat(verifier, mainMessage + "\n Previous Response: " + previousResponse.parsed().asAi());
            if (newResponse.isError()) {
                FileLogger.LOGGER.warning("Error in verifier " + previousResponse.error());
                continue;
            }

            ApplicableDiff parsedNewResponse = newResponse.parsed();
            if (parsedNewResponse.isUnchanged()) continue;

            previousResponse = newResponse;
            String name = verifiersAi.get(i).defaultRequestParameters().modelName();
            notes.addAll(
                    parsedNewResponse.notes()
                            .stream()
                            .map(it -> name + " : " + it)
                            .toList()
            );
            System.out.println(previousResponse);
        }

        if (previousResponse.isError()) {
            return previousResponse;
        }

        return ValidatedResponse.fromDiff(previousResponse.parsed().withNotes(notes));
    }

    // Message calc + run of message
    public abstract ValidatedResponse ask();
}
