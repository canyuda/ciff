package com.ciff.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingScheduler {

    private static final long STUCK_THRESHOLD_MINUTES = 5;

    private final DocumentMapper documentMapper;
    private final DocumentProcessingService documentProcessingService;

    @Scheduled(fixedDelay = 60_000)
    public void retryStuckDocuments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);

        LambdaQueryWrapper<DocumentPO> wrapper = new LambdaQueryWrapper<DocumentPO>()
                .eq(DocumentPO::getStatus, "uploaded")
                .lt(DocumentPO::getUpdateTime, threshold);

        List<DocumentPO> stuck = documentMapper.selectList(wrapper);
        if (stuck.isEmpty()) {
            return;
        }

        log.info("Found {} stuck documents (uploaded > {}min), retrying", stuck.size(), STUCK_THRESHOLD_MINUTES);
        for (DocumentPO doc : stuck) {
            try {
                documentProcessingService.process(doc.getId());
            } catch (Exception e) {
                log.error("Failed to retry document: {}", doc.getId(), e);
            }
        }
    }
}
