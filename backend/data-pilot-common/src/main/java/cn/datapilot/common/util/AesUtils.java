package cn.datapilot.common.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/25
 * @since 1.0.0
 */
@Slf4j
public class AesUtils {

    public static final String TRANSFORMATION = "AES/ECB/PKCS5PADDING";
    public static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final String ALGORITHM = "AES";
    private static final String VERSION_PREFIX = "v2:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * AES加密数据
     *
     * @param text 明文
     * @return 密文 base64
     */
    public static String encrypt(String text, String secretKey) {
        try {
            if (StrUtil.isEmpty(text)) {
                return text;
            }
            SecretKeySpec sks = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            log.warn("AES加密失败", ex);
            return text;
        }
    }

    /**
     * 使用随机 IV 的 AES-GCM 加密，适用于新安全字段。
     */
    public static String encryptGcm(String text, String secretKey) {
        try {
            if (StrUtil.isEmpty(text)) {
                return text;
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            SecretKeySpec sks = new SecretKeySpec(deriveKey(secretKey), ALGORITHM);
            Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, sks, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("AES-GCM 加密失败", ex);
        }
    }

    /**
     * 解密
     *
     * @param encrypt 密文
     * @return 明文
     */
    public static String decrypt(String encrypt, String secretKey) {
        try {
            SecretKeySpec sks = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypt));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("AES解密失败", ex);
            return encrypt;
        }
    }

    /**
     * 解密由 {@link #encryptGcm(String, String)} 生成的版本化密文。
     */
    public static String decryptGcm(String encrypt, String secretKey) {
        if (StrUtil.isEmpty(encrypt)) {
            return encrypt;
        }
        try {
            if (!encrypt.startsWith(VERSION_PREFIX)) {
                throw new IllegalArgumentException("不是 AES-GCM 版本化密文");
            }
            byte[] payload = Base64.getDecoder().decode(encrypt.substring(VERSION_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("AES-GCM 密文长度不合法");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);
            SecretKeySpec sks = new SecretKeySpec(deriveKey(secretKey), ALGORITHM);
            Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, sks, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("AES-GCM 解密失败", ex);
        }
    }

    private static byte[] deriveKey(String secretKey) throws Exception {
        if (StrUtil.isBlank(secretKey)) {
            throw new IllegalArgumentException("AES 密钥不能为空");
        }
        return MessageDigest.getInstance("SHA-256").digest(secretKey.getBytes(StandardCharsets.UTF_8));
    }

}
