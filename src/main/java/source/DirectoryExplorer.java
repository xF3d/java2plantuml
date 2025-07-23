package source;

import java.io.File;
import java.io.FileFilter;

public class DirectoryExplorer {

    private final FileHandler fileHandler;

    public DirectoryExplorer (FileHandler fileHandler){
        this.fileHandler=fileHandler;
    }


    private static final FileFilter fileFilter = pathname -> pathname.toString().endsWith(".java");

    public void explore(File file) {

        if (!file.isDirectory()) {
            fileHandler.handle(file);
            return;
        }

        File[] files = file.listFiles();
        if (files == null) {
            return;
        }

        for(File f : files) {
            if(f.isDirectory() || fileFilter.accept(f)){
                explore(f);
            }
        }
    }

}
