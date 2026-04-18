package com.ciff.knowledge.service;

import java.io.InputStream;

/**
 * File storage abstraction for future OSS support.
 */
public interface FileStorage {

    /**
     * Store a file and return the storage path.
     *
     * @param category  sub-directory (e.g. "knowledge")
     * @param filename  target filename
     * @param content   file content stream
     * @param size      content size in bytes
     * @return storage path for later retrieval
     */
    String store(String category, String filename, InputStream content, long size);

    /**
     * Load a file as InputStream by its storage path.
     */
    InputStream load(String path);

    /**
     * Delete a file by its storage path.
     */
    void delete(String path);
}
