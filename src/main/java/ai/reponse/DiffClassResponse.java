package ai.reponse;

import translate.translator.SimpleTranslator;

import java.nio.file.Path;

public record DiffClassResponse(String clazz) implements ApplicableDiff{
    @Override
    public void apply(SimpleTranslator originalTranslator, Path toUpdatePath) {

    }

    @Override
    public Path getPath(SimpleTranslator translator) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String origin() {
        return clazz;
    }
}
