package ai.pipeline;

import ai.reponse.MessageValidator;
import ai.reponse.ValidatedResponse;
import ai.chat.BaseChat;
import file.Config;
import file.FileLogger;
import lombok.Getter;

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
            } catch (Exception e) {
                lastResponse = new ValidatedResponse(null, e.getMessage());
                continue;
            }

            String error = validator.validate(origin, stringResponse);
            if (error == null) {
                return new ValidatedResponse(stringResponse, null);
            } else {
                lastResponse = new ValidatedResponse(null, error);
            }
        }

        return lastResponse;
    }

    protected ValidatedResponse message(String mainMessage) {
        ValidatedResponse response = this.validatedChat(suggestion, mainMessage);

        if (!response.isValid()) {
            FileLogger.LOGGER.severe("Fatal error in generator: " + response.error());
            return null;
        }

        System.out.println(response);
        ValidatedResponse previousResponse = response;
        for (BaseChat verifier : verifiers) {
            previousResponse = this.validatedChat(verifier, mainMessage + "\n Previous Response: " + previousResponse.message());

            if (!previousResponse.isValid()) {
                FileLogger.LOGGER.warning("Error in verifier " + previousResponse.error());
                continue;
            }

            System.out.println(previousResponse);
        }

        return previousResponse;
    }

    // Message calc + run of message
    public abstract ValidatedResponse ask();
}
