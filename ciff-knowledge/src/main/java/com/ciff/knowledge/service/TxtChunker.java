package com.ciff.knowledge.service;

import java.util.ArrayList;
import java.util.List;

/**
 * TXT chunker optimized for Chinese text, with sentence-aware splitting
 * and configurable overlap for RAG retrieval quality.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Split content by double newline into paragraphs</li>
 *   <li>Merge short paragraphs while accumulated length ≤ chunkSize</li>
 *   <li>Split long paragraphs at sentence boundaries (。！？\n；)</li>
 *   <li>Add overlap between adjacent chunks</li>
 * </ol>
 */
public final class TxtChunker {

    /** Chinese sentence-ending punctuation */
    private static final String SENTENCE_SEPARATORS = "。！？\n；";

    private TxtChunker() {
    }

    /**
     * Split text content into chunks with overlap.
     *
     * @param content      raw text content
     * @param chunkSize    max characters per chunk
     * @param overlapChars overlap characters between adjacent chunks (0 to disable)
     * @return list of text chunks
     */
    public static List<String> chunk(String content, int chunkSize, int overlapChars) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        int overlap = Math.max(0, Math.min(overlapChars, chunkSize / 2));

        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> rawChunks = new ArrayList<>();
        StringBuilder accumulator = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > chunkSize) {
                flushAccumulator(rawChunks, accumulator);
                splitLongParagraph(trimmed, chunkSize, rawChunks);
            } else if (accumulator.length() + (accumulator.length() > 0 ? 1 : 0) + trimmed.length() <= chunkSize) {
                if (accumulator.length() > 0) {
                    accumulator.append('\n');
                }
                accumulator.append(trimmed);
            } else {
                flushAccumulator(rawChunks, accumulator);
                accumulator.append(trimmed);
            }
        }
        flushAccumulator(rawChunks, accumulator);

        if (overlap == 0 || rawChunks.size() <= 1) {
            return rawChunks;
        }
        return applyOverlap(rawChunks, overlap);
    }

    /**
     * Split text content into chunks with default overlap (max of chunkSize/10 and 50).
     */
    public static List<String> chunk(String content, int chunkSize) {
        return chunk(content, chunkSize, Math.max(chunkSize / 10, 50));
    }

    private static void splitLongParagraph(String text, int chunkSize, List<String> chunks) {
        int start = 0;
        while (start < text.length()) {
            if (start + chunkSize >= text.length()) {
                chunks.add(text.substring(start));
                break;
            }

            int end = findSplitPoint(text, start, chunkSize);
            chunks.add(text.substring(start, end));
            start = end;
        }
    }

    /**
     * Find the best split point within [start, start + chunkSize].
     * Prefers sentence boundaries; falls back to hard cut.
     */
    private static int findSplitPoint(String text, int start, int chunkSize) {
        int searchStart = start + (int) (chunkSize * 0.6);
        int searchEnd = start + chunkSize;

        int best = -1;
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            if (SENTENCE_SEPARATORS.indexOf(text.charAt(i)) >= 0) {
                best = i + 1;
                break;
            }
        }

        if (best > 0) {
            return best;
        }

        // Fall back: search from start+0.6 to end for any whitespace
        for (int i = searchEnd - 1; i >= searchStart; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }

        // No boundary found, hard cut
        return start + chunkSize;
    }

    private static List<String> applyOverlap(List<String> chunks, int overlap) {
        List<String> overlapped = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (i > 0 && chunk.length() > overlap) {
                String prev = chunks.get(i - 1);
                int overlapStart = Math.max(0, prev.length() - overlap);
                String overlapText = prev.substring(overlapStart);
                chunk = overlapText + chunk;
            }
            overlapped.add(chunk);
        }
        return overlapped;
    }

    private static void flushAccumulator(List<String> chunks, StringBuilder accumulator) {
        if (accumulator.length() > 0) {
            chunks.add(accumulator.toString());
            accumulator.setLength(0);
        }
    }
}
