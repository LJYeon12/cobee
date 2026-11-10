package org.example.memberservice.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    // 32바이트 대칭 키 생성
    public static SecretKey generateSymmetricKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom()); // AES-256
        return keyGen.generateKey();
    }

    // 공개 키로 대칭 키 암호화 (RSA/ECB/PKCS1Padding)
    public static String encryptSymmetricKey(SecretKey symmetricKey, String base64PublicKey) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(symmetricKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 대칭 키와 IV로 데이터 암호화 (AES/CBC/PKCS7Padding)
    public static byte[] encryptData(byte[] data, SecretKey symmetricKey) throws Exception {
        // BouncyCastleProvider 명시적 사용
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        // IV는 대칭 키의 처음 16바이트 사용
        IvParameterSpec ivSpec = new IvParameterSpec(symmetricKey.getEncoded(), 0, 16);
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivSpec);
        return cipher.doFinal(data);
    }

    // 대칭 키와 IV로 데이터 복호화 (AES/CBC/PKCS7Padding)
    public static String decryptData(String base64EncryptedData, SecretKey symmetricKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(base64EncryptedData);
        // BouncyCastleProvider 명시적 사용
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        // IV는 대칭 키의 처음 16바이트 사용
        IvParameterSpec ivSpec = new IvParameterSpec(symmetricKey.getEncoded(), 0, 16);
        cipher.init(Cipher.DECRYPT_MODE, symmetricKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }
}
