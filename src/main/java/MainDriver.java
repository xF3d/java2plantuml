import file.FileLogger;
import gui.frame.MainFrame;
import translate.translator.Config;

import javax.swing.*;
import java.net.URISyntaxException;

public class MainDriver {


    public static void main(String[] args) throws URISyntaxException {
        Config.init();
        FileLogger.init();
        SwingUtilities.invokeLater(MainFrame::new);
    }

}
