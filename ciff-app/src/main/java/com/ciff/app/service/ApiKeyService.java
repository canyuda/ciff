package com.ciff.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.app.dto.apikey.ApiKeyCreateRequest;
import com.ciff.app.dto.apikey.ApiKeyVO;
import com.ciff.app.entity.ApiKeyPO;
import com.ciff.app.entity.UserPO;
import com.ciff.agent.entity.AgentPO;
import com.ciff.agent.mapper.AgentMapper;
import com.ciff.app.mapper.ApiKeyMapper;
import com.ciff.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "ciff_";
    private static final int KEY_LENGTH = 32;

    private final ApiKeyMapper apiKeyMapper;
    private final AgentMapper agentMapper;
    private final UserService userService;

    public ApiKeyVO createKey(ApiKeyCreateRequest request) {
        String rawKey = generateRawKey();
        String keyHash = sha256(rawKey);
        String keyPrefix = rawKey.substring(0, Math.min(8, rawKey.length()));

        ApiKeyPO po = new ApiKeyPO();
        po.setUserId(UserContext.getUserId());
        po.setAgentId(request.getAgentId());
        po.setName(request.getName());
        po.setKeyHash(keyHash);
        po.setKeyPrefix(KEY_PREFIX + keyPrefix);
        po.setExpiresAt(request.getExpiresAt() != null ? request.getExpiresAt().toLocalDateTime() : null);
        po.setStatus("active");
        apiKeyMapper.insert(po);

        return toVO(po, rawKey);
    }

    public List<ApiKeyVO> listKeys() {
        Long userId = UserContext.getUserId();
        List<ApiKeyPO> keys = apiKeyMapper.selectList(
                new LambdaQueryWrapper<ApiKeyPO>()
                        .eq(ApiKeyPO::getUserId, userId)
                        .orderByDesc(ApiKeyPO::getCreateTime)
        );

        // batch lookup agent names
        List<Long> agentIds = keys.stream()
                .map(ApiKeyPO::getAgentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> agentNameMap = agentIds.isEmpty()
                ? Map.of()
                : agentMapper.selectBatchIds(agentIds).stream()
                        .collect(Collectors.toMap(AgentPO::getId, AgentPO::getName, (a, b) -> a));

        return keys.stream().map(k -> {
            ApiKeyVO vo = toVO(k, null);
            if (k.getAgentId() != null) {
                vo.setAgentName(agentNameMap.getOrDefault(k.getAgentId(), null));
            }
            return vo;
        }).toList();
    }

    public void revokeKey(Long id) {
        ApiKeyPO key = apiKeyMapper.selectById(id);
        if (key == null || !key.getUserId().equals(UserContext.getUserId())) {
            throw new IllegalArgumentException("API key not found");
        }
        key.setStatus("revoked");
        apiKeyMapper.updateById(key);
    }

    public ApiKeyPO validateKey(String rawKey) {
        String hash = sha256(rawKey);
        ApiKeyPO key = apiKeyMapper.selectOne(
                new LambdaQueryWrapper<ApiKeyPO>()
                        .eq(ApiKeyPO::getKeyHash, hash)
                        .eq(ApiKeyPO::getStatus, "active")
        );
        if (key == null) {
            return null;
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return key;
    }

    private String generateRawKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[KEY_LENGTH];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private ApiKeyVO toVO(ApiKeyPO po, String rawKey) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setKeyPrefix(po.getKeyPrefix());
        vo.setAgentId(po.getAgentId());
        vo.setStatus(po.getStatus());
        vo.setExpiresAt(po.getExpiresAt());
        vo.setCreateTime(po.getCreateTime());
        vo.setRawKey(rawKey);
        return vo;
    }
}
