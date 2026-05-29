package translate.translator;

import translate.ClassDiagramConfig;
import visitors.*;

public class TranslatorConfig {
    public static ClassDiagramConfig config;

    public static void initDefaults(Translator translator) {
        TranslatorConfig.config =
                new ClassDiagramConfig.Builder()
                        .withVisitor(new ClassVisitor(translator))
                        .withVisitor(new InterfaceVisitor(translator))
                        .withVisitor(new EnumVisitor(translator))
                        .withVisitor(new RecordVisitor(translator))
                        .withVisitor(new AnnotationVisitor(translator))
                        .setShowMethods(true)
                        .setShowAttributes(true)
                        .setShowColoredAccessSpecifiers(true)
                        .build();
    }
}
