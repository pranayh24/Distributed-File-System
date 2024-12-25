package org.prh.dfs.server;

import org.prh.dfs.model.FileMetaData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class DirectoryHandler {
    private final String storagePath;

    public DirectoryHandler(String storagePath) {
        this.storagePath = storagePath;
    }

    public List<FileMetaData> listDirectory(String path) throws IOException {
        Path fullPath = Paths.get(storagePath, path);
        if(!Files.exists(fullPath)) {
            throw new FileNotFoundException("Directory not found: " + path);
        }

        List<FileMetaData> files = new ArrayList<>();
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(fullPath)) {
            for(Path entry : stream) {
                files.add(new FileMetaData(
                        entry.getFileName().toString(),
                        entry.toString().substring(storagePath.length()),
                        Files.isDirectory(entry),
                        Files.size(entry),
                        new Date(Files.getLastModifiedTime(entry).toMillis())
                ));
            }
        }
        return files;
    }

    public boolean createDirectory(String path) throws IOException  {
        Path fullPath = Paths.get(storagePath, path);
        Files.createDirectories(fullPath);
        return true;
    }

    public boolean deleteDirectory(String path) throws IOException {
        Path fullPath = Paths.get(storagePath, path);
        if(!Files.exists(fullPath)) {
            return false;
        }
         Files.walk(fullPath)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch(IOException e) {
                         throw new UncheckedIOException(e);
                     }
                 });
         return true;
    }

    public boolean moveOrRename(String sourcePath, String destinationPath) throws IOException {
        Path source = Paths.get(storagePath, sourcePath);
        Path destination = Paths.get(storagePath, destinationPath);

        if(!Files.exists(source)) {
            return false;
        }

        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
