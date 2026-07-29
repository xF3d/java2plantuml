package gui.frame;

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
import translate.component.formatter.MemberFormatter;
import translate.translator.ComplexityTranslator;
import file.Config;
import util.PlantUMLParser;
import util.SignatureParser;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PlantUMLFrame extends JFrame {

    private final JSVGCanvas image;
    private final ComplexityTranslator complexityTranslator;
    private final String umlOutput;

    public PlantUMLFrame(String umlOutput, ComplexityTranslator complexityTranslator) throws IOException {
        super("PlantUML Display");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(1000, 600);

        this.umlOutput = umlOutput;
        this.complexityTranslator = complexityTranslator;
        Config.getInstance().updateTranslator(complexityTranslator);

        complexityTranslator.execute();
        String xmlSvgOutput = PlantUMLParser.puml2svg(getProblematicPuml());

        this.image = new JSVGCanvas();
        this.image.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);

        // Files
        File tempSvg = File.createTempFile("plantuml", ".svg");
        Files.writeString(tempSvg.toPath(), xmlSvgOutput, StandardCharsets.UTF_8);
        this.image.setURI(tempSvg.toURI().toString());

        JScrollPane scrollPane = new JScrollPane(image);

        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.getContentPane().add(scrollPane);

        setupImageListeners();
        setupMenu();

        this.setVisible(true);
        FrameManager.addFrame(this);
    }

    private String getProblematicPuml() {
        String problematicClassesFormatted = String.join("", complexityTranslator.getProblematicClassesUml());
        return umlOutput.replace("@enduml", "\n" + problematicClassesFormatted + "@enduml");
    }

    private void setupImageListeners() {
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
                    String foundClass = null;
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node textNode = childNodes.item(j);
                        if (!(textNode instanceof Element textElement)) continue;
                        if (!"text".equals(textElement.getTagName())) continue;

                        // we don't need listeners for external classes
                        String textContent = textElement.getTextContent();
                        if (textContent.startsWith("external ")) break;

                        if (foundClass == null) {
                            // For some reason the dataQualifiedName replaces $ with "." so small workaround , might not be necessary anymore
                            String parsedFQN = dataQualifiedName.substring(0, dataQualifiedName.length() - textContent.length()) + textContent;
                            String finalParsedFQN = parsedFQN.replace("..", MemberFormatter.PACKAGE_DELIMITER);

                            ((EventTarget) textElement).addEventListener("click", evt -> {
                                new ClassFrame(finalParsedFQN, complexityTranslator);
                            }, false);
                            foundClass = finalParsedFQN;
                        } else {
                            // only methods
                            if (!textContent.contains("(")) continue;

                            // ignore constructors
                            if(SignatureParser.isConstructor(textContent)) continue;

                            String extractSignature = SignatureParser.extractSignature(textContent);
                            String finalFoundClass = foundClass;
                            ((EventTarget) textElement).addEventListener("click", evt -> {
                                new MethodFrame(finalFoundClass, extractSignature, complexityTranslator);
                            }, false);
                        }
                    }
                }
            }
        });

        // fix svg being messed up for some reason
        this.image.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                image.revalidate();
                image.repaint();
            }
        });

        this.image.addMouseWheelListener(e -> {
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

        this.image.addKeyListener(new KeyListener() {
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

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu exportMenu = new JMenu("Export");
        menuBar.add(exportMenu);

        JMenuItem simplePuml = new JMenuItem("Simple puml");
        simplePuml.addActionListener(e -> {
            var saver = new SimpleFileSaver(umlOutput, ".puml");
            saver.showSave();
        });

        JMenuItem evaluatedPuml = new JMenuItem("Evaluated puml");
        evaluatedPuml.addActionListener(e -> {
            var saver = new SimpleFileSaver(getProblematicPuml(), ".puml");
            saver.showSave();
        });

        JMenuItem simpleSvg = new JMenuItem("Simple svg");
        simpleSvg.addActionListener(e -> {
            var saver = new SimpleFileSaver(PlantUMLParser.puml2svg(umlOutput), ".svg");
            saver.showSave();
        });

        JMenuItem evaluatedSvg = new JMenuItem("Evaluated svg");
        evaluatedSvg.addActionListener(e -> {
            var saver = new SimpleFileSaver(PlantUMLParser.puml2svg(getProblematicPuml()), ".svg");
            saver.showSave();
        });

        exportMenu.add(simplePuml);
        exportMenu.add(evaluatedPuml);
        exportMenu.add(simpleSvg);
        exportMenu.add(evaluatedSvg);

        this.setJMenuBar(menuBar);
    }

    private class SimpleFileSaver extends JFileChooser {
        private final String contents;
        private final String extension;

        public SimpleFileSaver(String contents, String extension) {
            super();
            this.contents = contents;
            this.extension = extension;
        }

        public void showSave() {
            if (this.showSaveDialog(PlantUMLFrame.this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = this.getSelectedFile();

            // Add extension if needed
            if (!file.getName().endsWith(extension)) {
                file = new File(file.getAbsolutePath() + extension);
            }

            // Confirm overwrite
            if (file.exists()) {
                int response = JOptionPane.showConfirmDialog(
                        this,
                        "Overwrite existing file?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION
                );

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Write the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(contents);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
