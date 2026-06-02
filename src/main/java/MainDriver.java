import file.FileLogger;
import gui.frame.MainFrame;

import javax.swing.*;

public class MainDriver {


    public static void main(String[] args) {
        FileLogger.init();
        SwingUtilities.invokeLater(MainFrame::new);
    }

}
