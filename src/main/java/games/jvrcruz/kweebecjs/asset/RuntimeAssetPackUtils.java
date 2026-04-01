package games.jvrcruz.kweebecjs.asset;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class RuntimeAssetPackUtils {
    private RuntimeAssetPackUtils() {
    }

    public static void rebuildArchive(Path sourceDir, Path archivePath) throws IOException {
        Files.createDirectories(archivePath.getParent());
        Files.deleteIfExists(archivePath);

        try (OutputStream outputStream = Files.newOutputStream(archivePath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            if (!Files.exists(sourceDir)) {
                return;
            }

            FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(sourceDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor);
        }
    }

    public static FileSystem openArchive(Path archivePath) throws IOException {
        return FileSystems.newFileSystem(archivePath, Map.of());
    }

    public static void syncArchive(Path sourceDir, Path archivePath) throws IOException {
        Files.createDirectories(archivePath.getParent());
        java.net.URI uri = java.net.URI.create("jar:" + archivePath.toUri());
        Map<String, String> env = Map.of("create", "true");
        try (FileSystem archive = FileSystems.newFileSystem(uri, env)) {
            Path archiveRoot = archive.getPath("/");
            Set<String> sourceRelativeFiles = new HashSet<>();
            if (Files.isDirectory(sourceDir)) {
                try (var sourcePaths = Files.walk(sourceDir)) {
                    for (Path sourceFile : sourcePaths.filter(Files::isRegularFile).toList()) {
                        String relative = sourceDir.relativize(sourceFile).toString().replace('\\', '/');
                        sourceRelativeFiles.add(relative);
                    }
                }
            }

            Set<String> archiveRelativeFiles = new HashSet<>();
            if (Files.exists(archiveRoot)) {
                try (var archivePaths = Files.walk(archiveRoot)) {
                    for (Path archiveFile : archivePaths.filter(Files::isRegularFile).toList()) {
                        String relative = archiveRoot.relativize(archiveFile).toString().replace('\\', '/');
                        archiveRelativeFiles.add(relative);
                    }
                }
            }

            for (String archiveFile : archiveRelativeFiles) {
                if (sourceRelativeFiles.contains(archiveFile)) {
                    continue;
                }
                Files.deleteIfExists(archive.getPath("/" + archiveFile));
            }

            for (String relative : sourceRelativeFiles) {
                Path sourceFile = sourceDir.resolve(relative);
                Path archiveFile = archive.getPath("/" + relative);
                Path parent = archiveFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.copy(sourceFile, archiveFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
