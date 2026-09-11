import com.formdev.flatlaf.FlatLightLaf;
import file.Config;
import file.FileLogger;
import gui.frame.MainFrame;
import serialization.JacksonMapper;

import javax.swing.*;


void main() {
    FlatLightLaf.setup();
    Config.init();
    JacksonMapper.init();
    FileLogger.init();
    SwingUtilities.invokeLater(MainFrame::new);
}
