package gui.jlist;

import translate.component.formatter.MemberFormatter;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Replaces the separators with "." for display, but keeps them internally
 */
public abstract class JListReferenced extends JList<String> {
    protected final String[] data;

    public JListReferenced(String[] data) {
        super(removeSeparators(data));
        this.data = data;
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // double-click
                    return;
                }

                int index = JListReferenced.this.locationToIndex(e.getPoint());
                if (index == -1) {
                    return;
                }

                String selectedItem = data[index];
                JListReferenced.this.onClick(selectedItem);
            }
        });
    }

    public static String[] removeSeparators(String[] data) {
        String[] output = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = data[i].replace(MemberFormatter.PACKAGE_DELIMITER, ".");
        }

        return output;
    }

    public abstract void onClick(String selectedItem);
}
