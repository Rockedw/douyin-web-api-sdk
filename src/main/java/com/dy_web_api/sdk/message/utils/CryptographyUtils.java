package com.dy_web_api.sdk.message.utils;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class CryptographyUtils {

    public static ECPrivateKey getDerivedKey(String privateKeyPem) throws Exception {
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", ""); // 使用\\s+替换所有空白字符

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static byte[] performECDH(String certificatePem, ECPrivateKey privateKey) throws Exception {
        // 修复证书解析
        String certificateContent = certificatePem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\\n", "\n") // 先将转义的换行符转为真正的换行符
                .replaceAll("\\s+", ""); // 然后移除所有空白字符

        byte[] certificateBytes = Base64.getDecoder().decode(certificateContent);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(new ByteArrayInputStream(certificateBytes));

        ECPublicKey publicKey = (ECPublicKey) certificate.getPublicKey();

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);

        byte[] sharedKey = keyAgreement.generateSecret();
        return hkdf(sharedKey, 32, "", "");
    }

    public static byte[] hkdf(byte[] inputKeyMaterial, int length, String salt, String info) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        // Extract阶段 - 修复salt处理
        byte[] saltBytes = salt.isEmpty() ? new byte[0] : salt.getBytes(StandardCharsets.UTF_8);

        // 如果salt为空，使用零字节数组作为key
        if (saltBytes.length == 0) {
            saltBytes = new byte[32]; // SHA256的输出长度
        }

        SecretKeySpec saltKey = new SecretKeySpec(saltBytes, "HmacSHA256");
        mac.init(saltKey);
        byte[] prk = mac.doFinal(inputKeyMaterial);

        // Expand阶段
        SecretKeySpec prkKey = new SecretKeySpec(prk, "HmacSHA256");
        mac.init(prkKey);

        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
        int hashLen = 32;
        int n = (int) Math.ceil((double) length / hashLen);

        byte[] okm = new byte[length];
        byte[] t = new byte[0];

        for (int i = 1; i <= n; i++) {
            mac.reset();
            mac.update(t);
            mac.update(infoBytes);
            mac.update((byte) i);
            t = mac.doFinal();

            int copyLength = Math.min(t.length, length - (i - 1) * hashLen);
            System.arraycopy(t, 0, okm, (i - 1) * hashLen, copyLength);
        }

        return okm;
    }

    public static String getClientData(byte[] derivedKeyByte, String tsSign, String ticket, String path, String timestamp) throws Exception {
        // 确保签名格式与Python版本一致
        String reqSignBefore = String.format("ticket=%s&path=%s&timestamp=%s", ticket, path, timestamp);

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(derivedKeyByte, "HmacSHA256");
        mac.init(secretKey);

        byte[] reqSignBytes = mac.doFinal(reqSignBefore.getBytes(StandardCharsets.UTF_8));
        String reqSign = bytesToHex(reqSignBytes);

        // 构建JSON对象
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("ts_sign", tsSign);
        jsonData.put("req_content", "ticket,path,timestamp");
        jsonData.put("req_sign", reqSign);
        jsonData.put("timestamp", timestamp);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(jsonData);

        // 移除不必要的字符串替换，除非确实需要
        // jsonString = jsonString.replace("cid: '' ", "_new: ");

        // Base64编码
        byte[] base64Bytes = Base64.getEncoder().encode(jsonString.getBytes(StandardCharsets.UTF_8));
        return new String(base64Bytes, StandardCharsets.UTF_8);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String test() throws Exception {
        // 示例用法
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/xuaaRID22Upof/ebuYVxe16wjJLpcCGlQIHrNf5toahRANCAAQCQvvbvyR//KXxePFbiJOtPzrzkFee/upo3U6oJPn36UsQ2PCAzLCapWoLunuEedEyuKw2h+06Ao1Hpb+9tAQp\n" +
                "-----END PRIVATE KEY-----";

        String certificatePem = "-----BEGIN CERTIFICATE-----\\nMIIEfTCCBCKgAwIBAgIUXWdS2tzmSoewCWfKFyiWMrJqs/0wCgYIKoZIzj0EAwIw\\nMTELMAkGA1UEBhMCQ04xIjAgBgNVBAMMGXRpY2tldF9ndWFyZF9jYV9lY2RzYV8y\\nNTYwIBcNMjIxMTE4MDUyMDA2WhgPMjA2OTEyMzExNjAwMDBaMCQxCzAJBgNVBAYT\\nAkNOMRUwEwYDVQQDEwxlY2llcy1zZXJ2ZXIwWTATBgcqhkjOPQIBBggqhkjOPQMB\\nBwNCAASE2llDPlfc8Rq+5J5HXhg4edFjPnCF3Ua7JBoiE/foP9m7L5ELIcvxCgEx\\naRCHbQ8kCCK/ArZ4FX/qCobZAkToo4IDITCCAx0wDgYDVR0PAQH/BAQDAgWgMDEG\\nA1UdJQQqMCgGCCsGAQUFBwMBBggrBgEFBQcDAgYIKwYBBQUHAwMGCCsGAQUFBwME\\nMCkGA1UdDgQiBCABydxqGrVEHhtkCWTb/vicGpDZPFPDxv82wiuywUlkBDArBgNV\\nHSMEJDAigCAypWfqjmRIEo3MTk1Ae3MUm0dtU3qk0YDXeZSXeyJHgzCCAZQGCCsG\\nAQUFBwEBBIIBhjCCAYIwRgYIKwYBBQUHMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0\\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL29jc3AwRgYIKwYBBQUH\\nMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl\\ncnRpZmljYXRlL29jc3AwdwYIKwYBBQUHMAKGa2h0dHA6Ly9uZXh1cy1wcm9kdWN0\\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL2Rvd25sb2FkLzQ4RjlD\\nMEU3QjBDNUE3MDVCOTgyQkU1NTE3MDVGNjQ1QzhDODc4QTguY3J0MHcGCCsGAQUF\\nBzAChmtodHRwOi8vbmV4dXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UubmV0L2FwaS9j\\nZXJ0aWZpY2F0ZS9kb3dubG9hZC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1\\nRjY0NUM4Qzg3OEE4LmNydDCB5wYDVR0fBIHfMIHcMGygaqBohmZodHRwOi8vbmV4\\ndXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UuY29tL2FwaS9jZXJ0aWZpY2F0ZS9jcmwv\\nNDhGOUMwRTdCMEM1QTcwNUI5ODJCRTU1MTcwNUY2NDVDOEM4NzhBOC5jcmwwbKBq\\noGiGZmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl\\ncnRpZmljYXRlL2NybC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1RjY0NUM4\\nQzg3OEE4LmNybDAKBggqhkjOPQQDAgNJADBGAiEAqMjT5ADMdGMeaImoJK4J9jzE\\nLqZ573rNjsT3k14pK50CIQCLpWHVKWi71qqqrMjiSDvUhpyO1DpTPRHlavPRuaNm\\nww==\\n-----END CERTIFICATE-----";

        // 获取私钥
        ECPrivateKey privateKey = getDerivedKey(privateKeyPem);

        // 执行ECDH并派生密钥
        byte[] derivedKey = performECDH(certificatePem, privateKey);

        // 生成客户端数据
        String clientData = getClientData(
                derivedKey,
                "ts.2.aeb66e3758a06318f4e67da238c5ab113ef321a084a35cc9f762e9084cc834c4c4fbe87d2319cf05318624ceda14911ca406dedbebeddb2e30fce8d4fa02575d",
                "hash.e297itc/3i1a5K1EU+B09x9KinB3Ngw+9ymMyDiedbA=",
                "ticket,path,timestamp",
                String.valueOf(System.currentTimeMillis() / 1000)
        );
        return clientData;
    }

    // 测试方法
    public static void main(String[] args) {
        try {
            // 示例用法
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                    "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/xuaaRID22Upof/ebuYVxe16wjJLpcCGlQIHrNf5toahRANCAAQCQvvbvyR//KXxePFbiJOtPzrzkFee/upo3U6oJPn36UsQ2PCAzLCapWoLunuEedEyuKw2h+06Ao1Hpb+9tAQp\n" +
                    "-----END PRIVATE KEY-----";

            String certificatePem = "-----BEGIN CERTIFICATE-----\\nMIIEfTCCBCKgAwIBAgIUXWdS2tzmSoewCWfKFyiWMrJqs/0wCgYIKoZIzj0EAwIw\\nMTELMAkGA1UEBhMCQ04xIjAgBgNVBAMMGXRpY2tldF9ndWFyZF9jYV9lY2RzYV8y\\nNTYwIBcNMjIxMTE4MDUyMDA2WhgPMjA2OTEyMzExNjAwMDBaMCQxCzAJBgNVBAYT\\nAkNOMRUwEwYDVQQDEwxlY2llcy1zZXJ2ZXIwWTATBgcqhkjOPQIBBggqhkjOPQMB\\nBwNCAASE2llDPlfc8Rq+5J5HXhg4edFjPnCF3Ua7JBoiE/foP9m7L5ELIcvxCgEx\\naRCHbQ8kCCK/ArZ4FX/qCobZAkToo4IDITCCAx0wDgYDVR0PAQH/BAQDAgWgMDEG\\nA1UdJQQqMCgGCCsGAQUFBwMBBggrBgEFBQcDAgYIKwYBBQUHAwMGCCsGAQUFBwME\\nMCkGA1UdDgQiBCABydxqGrVEHhtkCWTb/vicGpDZPFPDxv82wiuywUlkBDArBgNV\\nHSMEJDAigCAypWfqjmRIEo3MTk1Ae3MUm0dtU3qk0YDXeZSXeyJHgzCCAZQGCCsG\\nAQUFBwEBBIIBhjCCAYIwRgYIKwYBBQUHMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0\\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL29jc3AwRgYIKwYBBQUH\\nMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl\\ncnRpZmljYXRlL29jc3AwdwYIKwYBBQUHMAKGa2h0dHA6Ly9uZXh1cy1wcm9kdWN0\\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL2Rvd25sb2FkLzQ4RjlD\\nMEU3QjBDNUE3MDVCOTgyQkU1NTE3MDVGNjQ1QzhDODc4QTguY3J0MHcGCCsGAQUF\\nBzAChmtodHRwOi8vbmV4dXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UubmV0L2FwaS9j\\nZXJ0aWZpY2F0ZS9kb3dubG9hZC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1\\nRjY0NUM4Qzg3OEE4LmNydDCB5wYDVR0fBIHfMIHcMGygaqBohmZodHRwOi8vbmV4\\ndXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UuY29tL2FwaS9jZXJ0aWZpY2F0ZS9jcmwv\\nNDhGOUMwRTdCMEM1QTcwNUI5ODJCRTU1MTcwNUY2NDVDOEM4NzhBOC5jcmwwbKBq\\noGiGZmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl\\ncnRpZmljYXRlL2NybC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1RjY0NUM4\\nQzg3OEE4LmNybDAKBggqhkjOPQQDAgNJADBGAiEAqMjT5ADMdGMeaImoJK4J9jzE\\nLqZ573rNjsT3k14pK50CIQCLpWHVKWi71qqqrMjiSDvUhpyO1DpTPRHlavPRuaNm\\nww==\\n-----END CERTIFICATE-----";

            // 获取私钥
            ECPrivateKey privateKey = getDerivedKey(privateKeyPem);

            // 执行ECDH并派生密钥
            byte[] derivedKey = performECDH(certificatePem, privateKey);

            // 生成客户端数据
            String clientData = getClientData(
                    derivedKey,
                    "ts.2.5bfb61509ba4e7a943adee595e4b1a2fe9ca1408bf895b43eb36193c7fe18fa6c4fbe87d2319cf05318624ceda14911ca406dedbebeddb2e30fce8d4fa02575d",
                    "hash.pXilKBTWV4KI/N0eqq5Vns+PwbXr7ls2jkIDDIBbAQU=",
                    "/aweme/v1/web/comment/list/",
                    String.valueOf(System.currentTimeMillis() / 1000)
            );

            System.out.println("Client Data: " + clientData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}