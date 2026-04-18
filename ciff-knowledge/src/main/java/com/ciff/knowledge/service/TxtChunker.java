package com.ciff.knowledge.service;

import java.util.ArrayList;
import java.util.List;

/**
 * TXT fixed-length chunker with paragraph-aware splitting.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Split content by double newline ("\n\n") into paragraphs</li>
 *   <li>Merge short paragraphs while accumulated length ≤ chunkSize</li>
 *   <li>Hard-split paragraphs longer than chunkSize</li>
 *   <li>Skip empty/blank content</li>
 * </ol>
 */
public final class TxtChunker {

    private TxtChunker() {
    }

    /**
     * Split text content into chunks.
     *
     * @param content   raw text content
     * @param chunkSize max characters per chunk
     * @return list of text chunks
     */
    public static List<String> chunk(String content, int chunkSize) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String[] paragraphs = content.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder accumulator = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > chunkSize) {
                // Flush accumulator first
                flushAccumulator(chunks, accumulator);
                // Hard-split long paragraph
                hardSplit(trimmed, chunkSize, chunks);
            } else if (accumulator.length() + trimmed.length() + 1 <= chunkSize) {
                // Append to accumulator
                if (accumulator.length() > 0) {
                    accumulator.append('\n');
                }
                accumulator.append(trimmed);
            } else {
                // Accumulator would overflow, flush and start new
                flushAccumulator(chunks, accumulator);
                accumulator.append(trimmed);
            }
        }

        // Flush remaining
        flushAccumulator(chunks, accumulator);
        return chunks;
    }

    private static void flushAccumulator(List<String> chunks, StringBuilder accumulator) {
        if (accumulator.length() > 0) {
            chunks.add(accumulator.toString());
            accumulator.setLength(0);
        }
    }

    private static void hardSplit(String text, int chunkSize, List<String> chunks) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }
    }
}
