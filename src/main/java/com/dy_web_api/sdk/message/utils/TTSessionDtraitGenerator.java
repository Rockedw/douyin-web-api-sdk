package com.dy_web_api.sdk.message.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;

public class TTSessionDtraitGenerator {

    // 固定的RSA公钥
    private static final String CENTRAL_RSA_PUB_BASE64 = "LS0tLS1CRUdJTiBSU0EgUFVCTElDIEtFWS0tLS0tCk1JSUJDZ0tDQVFFQTQrZHZ2WTd1TStvcGMrbkxHL0R1bVNlRm83YVZjSW0xTE8rbVVJcldwclJ6UDBhMUdwRVEKNHF0TzlNUmYvbHdFSXgzOCs0Qlo0WE9HemV2VnR1VXZmSU9VRTdBVHRRVzdGS0pmNVBuU0xDSTYvazB2bDFGQwpMVVNWbUVQNnFQSnJJalo0elhvcWkzeXVOWisxb2RiUkEvL0dIZ2NnU3l5eWFMcXp3amtwV0dYb3VNWW12WXNTCnBway9mdjJFV0FCc3RQTnhXYTRFT0JDYWRUVVBrWE5RNzZOQkVQOXh6ZkpTMjB3aUR2MW9TL3ZLdnJTVXBXY0oKbmF6a2tCdnFRYmJBcVZiUUZURi9EUGlrcHB1NlpUNmxHSVh2SktDcmVlRmlIQTJxSzZ0UzE4U1dWSFc5QVJ6MQorcGpCMWVxSUlZdG9oV3BUMkI0ME9DNE84dFZlQkFuYmlRSURBUUFCCi0tLS0tRU5EIFJTQSBQVUJMSUMgS0VZLS0tLS0=";

    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String CHARSET = "UTF-8";

    private static final Random random = new SecureRandom();

    /**
     * 生成x-tt-session-dtrait
     * @param urlPath 请求路径，默认为'/web/api/media/ameme/create_v2/'
     * @return dtrait字符串，格式为"d0_xx_xx"
     */
    public static String getSessionDtrait(String urlPath) {
        if (urlPath == null || urlPath.isEmpty()) {
            urlPath = "/web/api/media/ameme/create_v2/";
        }

        try {
            // 生成随机key和iv
            byte[] randomKeyBytes = generateRandomBytes(16);
            byte[] randomIvBytes = generateRandomBytes(16);
            String randomKey = bytesToHex(randomKeyBytes);
            String randomIv = bytesToHex(randomIvBytes);

            // 获取dtrait（这里需要你实现具体的dtrait生成逻辑）
            String dtrait = getDtrait();

            // 构建JSON数据
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("dtrait", dtrait);
            jsonData.put("timestamp", System.currentTimeMillis() / 1000);
            jsonData.put("subversion", "1.0.31-beta.4");
            jsonData.put("path", urlPath);

            String envData = mapToJsonString(jsonData);

            // RSA加密randomKey
            String rsaResult = rsaEncrypt(randomKey);

            // AES加密envData
            String aesResult = aesEncrypt(envData, randomKeyBytes, randomIvBytes);

            // 组合最终结果
            return String.format("d0_%s_%s", rsaResult, aesResult);

        } catch (Exception e) {
            throw new RuntimeException("生成session-dtrait失败", e);
        }
    }

    /**
     * RSA加密
     */
    private static String rsaEncrypt(String data) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(CENTRAL_RSA_PUB_BASE64);
        String publicKeyPem = new String(publicKeyBytes, CHARSET);
        publicKeyPem = publicKeyPem.replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] derBytes = Base64.getDecoder().decode(publicKeyPem);

        ASN1Sequence seq = ASN1Sequence.getInstance(derBytes);
        RSAPublicKey bcRsa = RSAPublicKey.getInstance(seq);
        java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(
                bcRsa.getModulus(), bcRsa.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(data.getBytes(CHARSET));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES加密
     */
    private static String aesEncrypt(String data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(data.getBytes(CHARSET));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 生成随机字节数组
     */
    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Map转JSON字符串（简化版本）
     */
    private static String mapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取dtrait值 - 需要根据实际情况实现
     */
    private static String getDtrait() {
        // 这里需要你根据实际情况实现dtrait的生成逻辑
        // 可能是从cookie、localStorage或其他地方获取
        return "example_dtrait_value";
    }

    // 测试方法
    public static void main(String[] args) {
        try {
            String dtrait = getSessionDtrait("/aweme/v1/web/comment/publish");
            System.out.println("生成的session-dtrait: " + dtrait);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}