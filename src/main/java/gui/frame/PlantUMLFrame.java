package gui.frame;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.svg.SVGDocument;
import source.DirectoryExplorer;
import source.FileHandler;
import translate.ClassDiagramConfig;
import translate.translator.ComplexityTranslator;
import translate.translator.TranslatorConfig;
import visitors.AnnotationVisitor;
import visitors.ClassVisitor;
import visitors.EnumVisitor;
import visitors.InterfaceVisitor;
import visitors.RecordVisitor;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PlantUMLFrame extends JFrame {

    private final JSVGCanvas image;
    private final ComplexityTranslator complexityTranslator;

    public PlantUMLFrame(String umlOutput) throws IOException {
        super("PlantUML Display");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(1000, 600);

        this.complexityTranslator = new ComplexityTranslator();
        TranslatorConfig.config =
            new ClassDiagramConfig.Builder()
                .withVisitor(new ClassVisitor(complexityTranslator))
                .withVisitor(new InterfaceVisitor(complexityTranslator))
                .withVisitor(new EnumVisitor(complexityTranslator))
                .withVisitor(new RecordVisitor(complexityTranslator))
                .withVisitor(new AnnotationVisitor(complexityTranslator))
                .setShowMethods(true)
                .setShowAttributes(true)
                .setShowColoredAccessSpecifiers(false)
                .build();

        FileHandler handler = new FileHandler(complexityTranslator);

        var selectedDirectory = MainFrame.getSelectedDirectory();
        if (selectedDirectory != null && selectedDirectory.exists()) {
            new DirectoryExplorer(handler).explore(selectedDirectory);
        }

        String problematicClassesFormatted = String.join("", complexityTranslator.getProblematicClassesUml());
        String enrichedUmlOutput = umlOutput.replace("@enduml", "\n" + problematicClassesFormatted + "@enduml");

        SourceStringReader ssr = new SourceStringReader(enrichedUmlOutput);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ssr.outputImage(os, new FileFormatOption(FileFormat.SVG));
        String xmlSvgOutput = os.toString();

        this.image = new JSVGCanvas();
        this.image.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);

        this.image.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
            @Override
            public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
                SVGDocument doc = e.getSVGDocument();

                NodeList gNodes = doc.getElementsByTagName("g");
                for (int i = 0; i < gNodes.getLength(); i++) {
                    Element gElement = (Element) gNodes.item(i);

                    String attributeClass = gElement.getAttribute("class");
                    if (!"entity".equals(attributeClass)) continue;

                    String dataQualifiedName = gElement.getAttribute("data-qualified-name");
                    if (dataQualifiedName.isEmpty()) continue;

                    var childNodes = gElement.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node textNode = childNodes.item(j);
                        if (!(textNode instanceof Element textElement)) continue;
                        if (!"text".equals(textElement.getTagName())) continue;

                        // For some reason the dataQualifiedName replaces $ with "." so small workaround
                        String className = textElement.getTextContent();
                        String parsedFQN = dataQualifiedName.substring(0, dataQualifiedName.length() - className.length()) + className;

                        ((EventTarget) textElement).addEventListener("click", evt -> {
                            new ClassFrame(parsedFQN, complexityTranslator);
                        }, false);
                        break;
                    }
                }
            }
        });

        // Files
        File puml = new File("output.puml");
        try (FileOutputStream fos = new FileOutputStream(puml)) {
            fos.write(enrichedUmlOutput.getBytes());
        }
        File tempSvg = File.createTempFile("plantuml", ".svg");
        Files.writeString(tempSvg.toPath(), xmlSvgOutput, StandardCharsets.UTF_8);
        this.image.setURI(tempSvg.toURI().toString());

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
