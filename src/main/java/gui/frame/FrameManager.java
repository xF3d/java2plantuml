package gui.frame;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class FrameManager {
    private static final List<JFrame> frames = new ArrayList<>();

    public static void addFrame(JFrame frame) {
        frames.add(frame);
    }

    public static void closeAll() {
        frames.forEach(
                (f) -> f.dispatchEvent(new WindowEvent(f, WindowEvent.WINDOW_CLOSING))
        );
        frames.clear();
    }
}
