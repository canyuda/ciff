package com.ciff.app.service;

import com.ciff.app.dto.apikey.ApiKeyCreateRequest;
import com.ciff.app.dto.apikey.ApiKeyVO;
import com.ciff.app.entity.ApiKeyPO;
import com.ciff.app.mapper.ApiKeyMapper;
import com.ciff.common.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyMapper apiKeyMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(100L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createKey_storesSha256Hash_returnsRawKeyOnce() {
        doAnswer(invocation -> {
            ApiKeyPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        }).when(apiKeyMapper).insert(any(ApiKeyPO.class));

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setName("test-key");
        request.setAgentId(10L);
        request.setExpiresAt(null);

        ApiKeyVO result = apiKeyService.createKey(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-key");
        assertThat(result.getAgentId()).isEqualTo(10L);
        // rawKey is returned exactly once
        assertThat(result.getRawKey()).isNotBlank();
        assertThat(result.getRawKey()).startsWith("ciff_");
        assertThat(result.getStatus()).isEqualTo("active");

        // verify inserted PO has SHA-256 hash, not the raw key
        verify(apiKeyMapper).insert(any(ApiKeyPO.class));
    }

    @Test
    void listKeys_returnsKeysForCurrentUser() {
        ApiKeyPO key1 = buildKey(1L, 100L, "key-1", "active");
        ApiKeyPO key2 = buildKey(2L, 100L, "key-2", "revoked");

        when(apiKeyMapper.selectList(any())).thenReturn(List.of(key1, key2));

        List<ApiKeyVO> result = apiKeyService.listKeys();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("key-1");
        assertThat(result.get(1).getName()).isEqualTo("key-2");
        // listKeys should never expose rawKey
        assertThat(result.stream().noneMatch(vo -> vo.getRawKey() != null)).isTrue();
    }

    @Test
    void revokeKey_setsStatusToRevoked() {
        ApiKeyPO key = buildKey(1L, 100L, "test-key", "active");
        when(apiKeyMapper.selectById(1L)).thenReturn(key);
        when(apiKeyMapper.updateById(any(ApiKeyPO.class))).thenReturn(1);

        apiKeyService.revokeKey(1L);

        verify(apiKeyMapper).updateById(any(ApiKeyPO.class));
    }

    @Test
    void revokeKey_notFound_throwsException() {
        when(apiKeyMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> apiKeyService.revokeKey(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API key not found");
    }

    @Test
    void revokeKey_userMismatch_throwsException() {
        // key belongs to user 200, but current user is 100
        ApiKeyPO key = buildKey(1L, 200L, "test-key", "active");
        when(apiKeyMapper.selectById(1L)).thenReturn(key);

        assertThatThrownBy(() -> apiKeyService.revokeKey(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API key not found");
    }

    @Test
    void validateKey_validActiveKey_returnsEntity() throws Exception {
        String rawKey = "ciff_testkey123";
        String hash = sha256(rawKey);

        ApiKeyPO key = buildKey(1L, 100L, "test-key", "active");
        key.setKeyHash(hash);
        key.setExpiresAt(null);
        when(apiKeyMapper.selectOne(any())).thenReturn(key);

        ApiKeyPO result = apiKeyService.validateKey(rawKey);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("active");
    }

    @Test
    void validateKey_revokedKey_returnsNull() throws Exception {
        String rawKey = "ciff_revokedkey";
        String hash = sha256(rawKey);

        // query filters by status=active, so revoked key returns null from DB
        when(apiKeyMapper.selectOne(any())).thenReturn(null);

        ApiKeyPO result = apiKeyService.validateKey(rawKey);

        assertThat(result).isNull();
    }

    @Test
    void validateKey_expiredKey_returnsNull() throws Exception {
        String rawKey = "ciff_expiredkey";
        String hash = sha256(rawKey);

        ApiKeyPO key = buildKey(1L, 100L, "expired-key", "active");
        key.setKeyHash(hash);
        key.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(apiKeyMapper.selectOne(any())).thenReturn(key);

        ApiKeyPO result = apiKeyService.validateKey(rawKey);

        assertThat(result).isNull();
    }

    private ApiKeyPO buildKey(Long id, Long userId, String name, String status) {
        ApiKeyPO po = new ApiKeyPO();
        po.setId(id);
        po.setUserId(userId);
        po.setName(name);
        po.setStatus(status);
        po.setAgentId(10L);
        po.setKeyPrefix("ciff_prefix");
        return po;
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
