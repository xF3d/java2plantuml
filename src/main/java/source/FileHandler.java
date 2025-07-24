package source;

import translate.Translator;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;

public class FileHandler {

    private final Translator translator;
    private final JTextArea output;

    public FileHandler(Translator translator, JTextArea output) {
        this.translator = translator;
        this.output = output;
    }


    void handle(File f) {

        output.append("File Found: " + f.getName() + "\n");

        try {
            translator.translateFile(new File(f.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            translator.setError(true);
        }

    }


}
