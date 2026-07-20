package util;

import file.FileLogger;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class PlantUMLParser {

    public static String puml2svg(String puml) {
        SourceStringReader ssr = new SourceStringReader(puml);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ssr.outputImage(os, new FileFormatOption(FileFormat.SVG));
        } catch (IOException ex) {
            FileLogger.LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            ex.printStackTrace();
        }
        return os.toString();
    }
}
