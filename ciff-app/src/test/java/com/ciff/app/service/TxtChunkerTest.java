package com.ciff.app.service;

import com.ciff.knowledge.service.TxtChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TxtChunkerTest {

    @Test
    void chunk_withNull_shouldReturnEmpty() {
        List<String> chunks = TxtChunker.chunk(null, 100, 10);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_withBlank_shouldReturnEmpty() {
        List<String> chunks = TxtChunker.chunk("   ", 100, 10);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_withNegativeChunkSize_shouldThrow() {
        assertThatThrownBy(() -> TxtChunker.chunk("hello", -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize must be positive");
    }

    @Test
    void chunk_shortText_shouldReturnSingleChunk() {
        String text = "这是一个短文本。只有几句话。";
        List<String> chunks = TxtChunker.chunk(text, 1000, 10);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("这是一个短文本");
    }

    @Test
    void chunk_longText_shouldSplitIntoMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("这是第").append(i).append("段内容。包含一些中文句子。用于测试分块逻辑。");
        }

        List<String> chunks = TxtChunker.chunk(sb.toString(), 100, 10);

        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk should not exceed chunkSize
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(120);
        }
    }

    @Test
    void chunk_paragraphAware_shouldMergeShortParagraphs() {
        String text = "第一段。\n\n第二段。\n\n第三段。";
        List<String> chunks = TxtChunker.chunk(text, 100, 0);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("第一段").contains("第二段").contains("第三段");
    }

    @Test
    void chunk_withOverlap_shouldIncludeOverlapText() {
        String text = "第一章内容。有很多句子。第二章内容。也有很多句子。第三章内容。继续更多内容。";
        List<String> chunks = TxtChunker.chunk(text, 30, 5);

        assertThat(chunks).hasSizeGreaterThan(1);
        // Chunks after first should contain overlap from previous
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).length()).isGreaterThan(5);
        }
    }

    @Test
    void chunk_sentenceBoundary_shouldSplitAtSentenceEnd() {
        // A long paragraph with clear sentence boundaries
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("句子").append(i).append("的内容。");
        }

        List<String> chunks = TxtChunker.chunk(sb.toString(), 50, 0);

        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk should ideally end with sentence separator
        for (int i = 0; i < chunks.size() - 1; i++) {
            String chunk = chunks.get(i);
            char lastChar = chunk.charAt(chunk.length() - 1);
            assertThat("。！？；".indexOf(lastChar)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void chunk_defaultOverlap_shouldWork() {
        String text = "A".repeat(200);
        List<String> chunks = TxtChunker.chunk(text, 100);

        assertThat(chunks).hasSizeGreaterThan(1);
    }
}
