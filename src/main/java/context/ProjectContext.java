package context;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import translate.translator.GroupedTranslators;

import javax.swing.*;
import java.io.File;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectContext {

    private final File selectedDirectory;
    private final GroupedTranslators groupedTranslators = new GroupedTranslators();

    @Getter
    private static ProjectContext instance = null;

    public static void init(File selectedDirectory) {
        instance = new ProjectContext(selectedDirectory);
        instance.calculate();

    }

    public static void recalculate() {
        instance = instance.recalculateInternal();
    }

    private ProjectContext recalculateInternal() {
        var temp = new ProjectContext(selectedDirectory);
        temp.calculate();
        return temp;
    }

    private void calculate() {
        boolean result = true;
        result &= this.groupedTranslators.getUmlTranslator().execute();
        result &= this.groupedTranslators.getComplexityTranslator().execute();

        if (!result) {
            JOptionPane.showMessageDialog(
                    null,
                    "Selected folder is invalid.",
                    "Invalid Folder",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
