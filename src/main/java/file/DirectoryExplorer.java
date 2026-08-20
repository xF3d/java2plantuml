package file;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class DirectoryExplorer {

    private final FileHandler fileHandler;

    public DirectoryExplorer (FileHandler fileHandler){
        this.fileHandler=fileHandler;
    }


    private static final FileFilter fileFilter = pathname -> pathname.toString().endsWith(".java");

    public void explore(File root) {
        Deque<File> stack = new ArrayDeque<>();
        stack.push(root);
        Path rootPath = root.toPath();

        boolean skipTests = Config.getInstance().getConfig().isSkipTests();
        ArrayList<File> filesFound = new ArrayList<>();
        while (!stack.isEmpty()) {
            File file = stack.pop();

            if (skipTests && rootPath.relativize(file.toPath()).toString().contains("test")) continue;

            if (!file.isDirectory()) {
                filesFound.add(file);
                continue;
            }

            File[] files = file.listFiles();
            if (files == null) {
                continue;
            }

            for (File f : files) {
                if (f.isDirectory() || fileFilter.accept(f)) {
                    stack.push(f);
                }
            }
        }

        fileHandler.handle(filesFound);
    }
}
