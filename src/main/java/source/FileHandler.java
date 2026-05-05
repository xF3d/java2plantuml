package source;

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            translator.setError(true);
        }
    }
}
