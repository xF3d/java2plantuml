package source;

import translate.translator.Translator;

import java.io.File;
import java.io.FileNotFoundException;

public class FileHandler {

    private final Translator translator;

    public FileHandler(Translator translator) {
        this.translator = translator;
    }


    void handle(File f) {
        try {
            translator.translateFile(new File(f.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            translator.setError(true);
        }

    }


}
