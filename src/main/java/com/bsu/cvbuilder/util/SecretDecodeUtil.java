package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@UtilityClass
public class SecretDecodeUtil {

    private static final String ALGORITHM = "AES";

    public static String decode(String encryptedToken, String signature) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, prepareKey(signature));
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting token", e);
        }
    }

    public static String encode(String plainToken, String signature) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, prepareKey(signature));
            byte[] encryptedBytes = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting token", e);
        }
    }

    private static SecretKeySpec prepareKey(String signature) {
        byte[] key = signature.getBytes(StandardCharsets.UTF_8);
        byte[] key16 = new byte[16];
        System.arraycopy(key, 0, key16, 0, Math.min(key.length, 16));
        return new SecretKeySpec(key16, ALGORITHM);
    }
}