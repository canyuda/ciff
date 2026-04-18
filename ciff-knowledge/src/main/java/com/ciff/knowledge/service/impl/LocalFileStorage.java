package com.ciff.knowledge.service.impl;

import com.ciff.knowledge.service.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    @Value("${ciff.upload.path:./uploads}")
    private String basePath;

    @Override
    public String store(String category, String filename, InputStream content, long size) {
        Path dir = Paths.get(basePath, category);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + dir, e);
        }

        Path target = dir.resolve(filename);
        try {
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + target, e);
        }
        return target.toString();
    }

    @Override
    public InputStream load(String path) {
        try {
            return Files.newInputStream(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", path, e);
        }
    }
}
