import file.FileLogger;
import gui.frame.MainFrame;
import file.Config;
import serialization.JacksonMapper;

import javax.swing.*;
import java.net.URISyntaxException;

public class MainDriver {


    public static void main(String[] args) throws URISyntaxException {
        Config.init();
        JacksonMapper.init();
        FileLogger.init();
        SwingUtilities.invokeLater(MainFrame::new);
    }

}
