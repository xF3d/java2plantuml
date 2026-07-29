package file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;

public class ResourceExtractor {

    private ResourceExtractor() {}

    public static Path extractToCurrentDirectory(String resourceName) throws IOException {
        return extract(resourceName, Paths.get("").resolve(resourceName));
    }

    public static Path extract(String resourceName, Path destination) throws IOException {
        return extract(resourceName, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Path extract(
            String resourceName,
            Path destination,
            CopyOption... options
    ) throws IOException {

        Objects.requireNonNull(resourceName);
        Objects.requireNonNull(destination);

        try (InputStream in = ResourceExtractor.class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {

            if (in == null) {
                throw new NoSuchFileException("Resource not found: " + resourceName);
            }

            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(in, destination, options);
            return destination.toAbsolutePath();
        }
    }
}
