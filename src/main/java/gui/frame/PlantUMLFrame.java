package gui.frame;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.util.XMLResourceDescriptor;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

public class PlantUMLFrame extends JFrame {

    private final JSVGCanvas image;

    public PlantUMLFrame(String umlOutput) throws IOException {
        super("PlantUML Display");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(1000, 600);

        SourceStringReader ssr = new SourceStringReader(umlOutput);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ssr.outputImage(os, new FileFormatOption(FileFormat.SVG));

        this.image = new JSVGCanvas();
        this.image.setURI(null);
        this.image.setSVGDocument(new SAXSVGDocumentFactory(
            XMLResourceDescriptor.getXMLParserClassName()
        ).createSVGDocument(null, new StringReader(os.toString())));

        JScrollPane scrollPane = new JScrollPane(image);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.getContentPane().add(scrollPane);

        // fix svg being messed up for some reason
        this.image.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                image.revalidate();
                image.repaint();
            }
        });

        setupZooming();

        this.setVisible(true);
        FrameManager.addFrame(this);
    }

    private void setupZooming() {
        image.addMouseWheelListener(e -> {
            if (!e.isControlDown()) return;

            e.consume();

            double scale = image.getRenderingTransform().getScaleX();

            double delta = (e.getPreciseWheelRotation() < 0) ? 1.1 : 0.9;
            double newScale = scale * delta;

            newScale = Math.max(0.1, Math.min(newScale, 10));

            AffineTransform at = new AffineTransform();
            at.scale(newScale, newScale);

            image.setRenderingTransform(at);
        });

        image.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (!e.isControlDown()) return;
                int keyCode = e.getKeyCode();
                if (keyCode != KeyEvent.VK_PLUS && keyCode != KeyEvent.VK_MINUS) return;

                e.consume();

                double scale = image.getRenderingTransform().getScaleX();

                double delta = (keyCode == KeyEvent.VK_PLUS) ? 1.1 : 0.9;
                double newScale = scale * delta;

                newScale = Math.max(0.1, Math.min(newScale, 10));

                AffineTransform at = new AffineTransform();
                at.scale(newScale, newScale);

                image.setRenderingTransform(at);
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }
}
