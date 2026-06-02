package file;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileLogger {
    public static final Logger LOGGER = Logger.getLogger(FileLogger.class.getName());

    public static void init() {
        try {
            FileHandler fileHandler = new FileHandler("log.txt", true);
            fileHandler.setFormatter(new Formatter() {
                private final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

                @Override
                public String format(LogRecord record) {
                    return String.format(
                        "%s [%s] %s%n",
                        formatter.format(Instant.ofEpochMilli(record.getMillis())),
                        record.getLevel(),
                        record.getMessage()
                    );
                }
            });

            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
