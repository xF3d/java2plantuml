package file;

import translate.translator.Translator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class FileHandler {

    private final Translator translator;

    public FileHandler(Translator translator) {
        this.translator = translator;
    }

    void handle(List<File> fileList) {
        try {
            translator.translateFiles(fileList);
        } catch (Exception e) {
            FileLogger.LOGGER.severe("Problem in handling files: " + e.getMessage());
        }
    }
}
