package file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

public final class FolderSync {

    private FolderSync() {}

    public static void sync(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source does not exist: " + source);
        }

        if (Files.isRegularFile(source)) {
            syncFile(source, destination);
            return;
        }

        Files.createDirectories(destination);

        // Track children that should exist
        Set<String> expectedNames;

        try (var stream = Files.list(source)) {
            expectedNames = stream
                    .filter(p -> !shouldIgnore(p))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        // Copy/update everything from source
        try (var stream = Files.list(source)) {
            for (Path srcChild : stream.toList()) {
                if (shouldIgnore(srcChild)) {
                    continue;
                }

                Path dstChild = destination.resolve(srcChild.getFileName());

                if (Files.isDirectory(srcChild)) {
                    sync(srcChild, dstChild);
                } else {
                    syncFile(srcChild, dstChild);
                }
            }
        }

        // Remove anything not present in source
        try (var stream = Files.list(destination)) {
            for (Path dstChild : stream.toList()) {
                if (shouldIgnore(dstChild)) {
                    continue;
                }

                if (!expectedNames.contains(dstChild.getFileName().toString())) {
                    deleteRecursively(dstChild);
                }
            }
        }
    }

    private static void syncFile(Path source, Path destination) throws IOException {

        if (!Files.exists(destination)) {
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }

        if (Files.isDirectory(destination)) {
            deleteRecursively(destination);
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }

        // Quick check: different sizes means definitely different.
        if (Files.size(source) != Files.size(destination)) {
            Files.copy(source, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }

        // Same size -> compare hashes.
        if (!sha256(source).equals(sha256(destination))) {
            Files.copy(source, destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean shouldIgnore(Path path) {
        String name = path.getFileName().toString();
        if (name.equals(".git")) return true;

        return name.endsWith(".lock") || name.endsWith(".lck") || name.endsWith(".tmp");
    }
}
