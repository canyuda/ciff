package com.ciff.workflow.engine.step;

import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.facade.KnowledgeFacade;
import com.ciff.workflow.dto.StepDefinition;
import com.ciff.workflow.engine.WorkflowContext;
import com.ciff.workflow.engine.dto.WorkflowExecutionResult.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetrievalStepExecutor implements StepExecutor {

    private final KnowledgeFacade knowledgeFacade;

    @Override
    public StepResult execute(StepDefinition step, WorkflowContext context) {
        Map<String, Object> config = step.getConfig();
        Long knowledgeBaseId = toLong(config.get("knowledgeBaseId"));
        String query = context.interpolate((String) config.getOrDefault("query", ""));
        int topK = config.get("topK") instanceof Number n ? n.intValue() : 3;

        try {
            List<KnowledgeChunkPO> chunks = knowledgeFacade.retrieve(query, List.of(knowledgeBaseId), topK, false);

            List<Map<String, Object>> results = chunks.stream().map(chunk -> {
                Map<String, Object> map = new HashMap<>();
                map.put("content", chunk.getContent());
                map.put("score", chunk.getSimilarity());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> mappedOutputs = mapOutputs(Map.of("result", results), step.getOutputs());

            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(true)
                    .outputs(mappedOutputs)
                    .build();
        } catch (Exception e) {
            log.error("Knowledge retrieval step {} failed", step.getId(), e);
            return StepResult.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .type(step.getType())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOutputs(Map<String, Object> outputs, Map<String, String> outputMapping) {
        if (outputMapping == null || outputMapping.isEmpty()) {
            return outputs;
        }
        Map<String, Object> mapped = new HashMap<>();
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            Object value = outputs.get(entry.getKey());
            mapped.put(entry.getValue(), value);
        }
        return mapped;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
