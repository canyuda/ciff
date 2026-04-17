package com.ciff.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密工具。
 *
 * <p>应用场景：用户在页面上配置模型供应商（如 OpenAI、Claude）时填入 API Key，
 * 后端调用本工具加密后存入数据库（ProviderPO.apiKeyEncrypted 字段），
 * 实际发起 LLM 请求前再解密还原为明文使用。确保 API Key 不以明文落库。</p>
 *
 * <p>算法：AES/CBC/PKCS5Padding，密钥取配置前 16 字节。
 * 密文格式：Base64(随机IV + 密文)，每次加密产生不同密文。</p>
 *
 * <p>密钥配置项：{@code ciff.security.api-key-secret}，至少 16 字符。
 * 如需更换密钥，修改配置文件后重启，再通过管理接口逐条重新加密存量数据。</p>
 */
@Component
public class ApiKeyEncryptor {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec keySpec;

    public ApiKeyEncryptor(@Value("${ciff.security.api-key-secret:}") String secret) {
        this.keySpec = buildKeySpec(secret);
    }

    /**
     * 加密 API Key。输出 Base64(IV + 密文)。
     * 输入为 null 或空串时返回空串。
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    /**
     * 解密 API Key。输入 Base64(IV + 密文)，返回明文。
     * 输入为 null 或空串时返回空串。
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return "";
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < IV_LENGTH + 1) {
                throw new IllegalStateException("加密数据长度不合法");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }

    private SecretKeySpec buildKeySpec(String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException(
                    "配置项 ciff.security.api-key-secret 至少需要 16 个字符");
        }
        byte[] keyBytes = secret.substring(0, 16).getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
