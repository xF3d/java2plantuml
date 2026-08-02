package gui;

import javax.swing.*;

public class GuiUtils {
    public static void setVerticalLayout(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    }
}
