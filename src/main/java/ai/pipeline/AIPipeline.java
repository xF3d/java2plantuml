package ai.pipeline;

import ai.reponse.MessageValidator;
import ai.reponse.ValidatedResponse;
import ai.chat.BaseChat;
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
    protected final List<BaseChat> verifiers ;

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
                stringResponse = model.chat(message);
                System.out.println(stringResponse);
            } catch (Exception e) {
                lastResponse = ValidatedResponse.error(e.getMessage());
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

        for (int i = 0; i < verifiers.size(); i++) {
            BaseChat verifier = verifiers.get(i);

            previousResponse = this.validatedChat(verifier, mainMessage + "\n Previous Response: " + previousResponse.parsed().asAi());

            if (previousResponse.isError()) {
                FileLogger.LOGGER.warning("Error in verifier " + previousResponse.error());
                continue;
            }

            String name = cfg.getVerifyModels().get(i).modelName();
            notes.addAll(
                    previousResponse.parsed().notes()
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
