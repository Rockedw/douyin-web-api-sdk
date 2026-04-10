package com.dy_web_api.sdk.message.utils;

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;

public class BdTicketGuard {

    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


    static String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/xuaaRID22Upof/ebuYVxe16wjJLpcCGlQIHrNf5toahRANCAAQCQvvbvyR//KXxePFbiJOtPzrzkFee/upo3U6oJPn36UsQ2PCAzLCapWoLunuEedEyuKw2h+06Ao1Hpb+9tAQp\n" +
            "-----END PRIVATE KEY-----";

    static String certificatePem = "-----BEGIN CERTIFICATE-----" +
            "\nMIIEfTCCBCKgAwIBAgIUXWdS2tzmSoewCWfKFyiWMrJqs/0wCgYIKoZIzj0EAwIw" +
            "\nMTELMAkGA1UEBhMCQ04xIjAgBgNVBAMMGXRpY2tldF9ndWFyZF9jYV9lY2RzYV8y" +
            "\nNTYwIBcNMjIxMTE4MDUyMDA2WhgPMjA2OTEyMzExNjAwMDBaMCQxCzAJBgNVBAYT" +
            "\nAkNOMRUwEwYDVQQDEwxlY2llcy1zZXJ2ZXIwWTATBgcqhkjOPQIBBggqhkjOPQMB" +
            "\nBwNCAASE2llDPlfc8Rq+5J5HXhg4edFjPnCF3Ua7JBoiE/foP9m7L5ELIcvxCgEx" +
            "\naRCHbQ8kCCK/ArZ4FX/qCobZAkToo4IDITCCAx0wDgYDVR0PAQH/BAQDAgWgMDEG" +
            "\nA1UdJQQqMCgGCCsGAQUFBwMBBggrBgEFBQcDAgYIKwYBBQUHAwMGCCsGAQUFBwME" +
            "\nMCkGA1UdDgQiBCABydxqGrVEHhtkCWTb/vicGpDZPFPDxv82wiuywUlkBDArBgNV" +
            "\nHSMEJDAigCAypWfqjmRIEo3MTk1Ae3MUm0dtU3qk0YDXeZSXeyJHgzCCAZQGCCsG" +
            "\nAQUFBwEBBIIBhjCCAYIwRgYIKwYBBQUHMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0" +
            "\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL29jc3AwRgYIKwYBBQUH" +
            "\nMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl" +
            "\ncnRpZmljYXRlL29jc3AwdwYIKwYBBQUHMAKGa2h0dHA6Ly9uZXh1cy1wcm9kdWN0" +
            "\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL2Rvd25sb2FkLzQ4RjlD" +
            "\nMEU3QjBDNUE3MDVCOTgyQkU1NTE3MDVGNjQ1QzhDODc4QTguY3J0MHcGCCsGAQUF" +
            "\nBzAChmtodHRwOi8vbmV4dXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UubmV0L2FwaS9j" +
            "\nZXJ0aWZpY2F0ZS9kb3dubG9hZC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1" +
            "\nRjY0NUM4Qzg3OEE4LmNydDCB5wYDVR0fBIHfMIHcMGygaqBohmZodHRwOi8vbmV4" +
            "\ndXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UuY29tL2FwaS9jZXJ0aWZpY2F0ZS9jcmwv" +
            "\nNDhGOUMwRTdCMEM1QTcwNUI5ODJCRTU1MTcwNUY2NDVDOEM4NzhBOC5jcmwwbKBq" +
            "\noGiGZmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl" +
            "\ncnRpZmljYXRlL2NybC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1RjY0NUM4" +
            "\nQzg3OEE4LmNybDAKBggqhkjOPQQDAgNJADBGAiEAqMjT5ADMdGMeaImoJK4J9jzE" +
            "\nLqZ573rNjsT3k14pK50CIQCLpWHVKWi71qqqrMjiSDvUhpyO1DpTPRHlavPRuaNm" +
            "\nww==" +
            "\n-----END CERTIFICATE-----";

    static String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAkL7278kf/yl8XjxW4iTrT8685BXnv7qaN1OqCT59+lLENjwgMywmqVqC7p7hHnRMrisNoftOgKNR6W/vbQEKQ==\n" +
            "-----END PUBLIC KEY-----";

    /**
     * JavaScript j函数的Java实现 - Base64编码
     */
    public static String j(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length; i += 3) {
            int n = ((data[i] & 0xFF) << 16);
            if (i + 1 < data.length) n |= ((data[i + 1] & 0xFF) << 8);
            if (i + 2 < data.length) n |= (data[i + 2] & 0xFF);

            for (int j = 0; j < 4; j++) {
                if (8 * i + 6 * j <= 8 * data.length) {
                    result.append(BASE64_CHARS.charAt((n >>> (6 * (3 - j))) & 63));
                } else {
                    result.append("=");
                }
            }
        }
        return result.toString();
    }

    /**
     * JavaScript L函数的Java实现
     */
    public static String L(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isEmpty()) {
            return "";
        }
        return j(hexToBytes(generatePublicKeyHex(publicKeyPem)));
    }

    /**
     * 从PEM格式提取公钥的十六进制表示
     */
    public static String generatePublicKeyHex(String publicKeyPem) {
        try {
            String strippedKey = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decodedKey = Base64.getDecoder().decode(strippedKey);
            String hexStr = bytesToHex(decodedKey);

            // 截取从第52个字符开始的部分
            if (hexStr.length() > 52) {
                return hexStr.substring(52);
            }
            return hexStr;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取bd_ticket_guard_ree_public_key
     */
    public static String getBdTicketGuardReePublicKey(String publicKeyPem) {
        return L(publicKeyPem);
    }

    /**
     * 获取cookies中的bd_ticket_guard_client_data
     */
    public static String getCookiesBdTicketGuardClientData(String bdTicketGuardReePublicKey) {
        String jsonData = "{\"bd-ticket-guard-version\":2,\"bd-ticket-guard-iteration-version\":1," +
                "\"bd-ticket-guard-ree-public-key\":\"" + bdTicketGuardReePublicKey + "\"," +
                "\"bd-ticket-guard-web-version\":2}";

        String base64Data = Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));
        return base64Data.replace("=", "") + "%3D%3D";
    }

    /**
     * JavaScript y函数的Java实现 - 自定义Base64编码
     */
    public static String y(String hexString) {
        StringBuilder result = new StringBuilder();
        int t, n;

        // 处理每3个十六进制字符
        for (t = 0; t + 3 <= hexString.length(); t += 3) {
            n = Integer.parseInt(hexString.substring(t, t + 3), 16);
            result.append(BASE64_CHARS.charAt(n >> 6)).append(BASE64_CHARS.charAt(63 & n));
        }

        // 处理剩余字符
        if (t + 1 == hexString.length()) {
            n = Integer.parseInt(hexString.substring(t, t + 1), 16);
            result.append(BASE64_CHARS.charAt(n << 2));
        } else if (t + 2 == hexString.length()) {
            n = Integer.parseInt(hexString.substring(t, t + 2), 16);
            result.append(BASE64_CHARS.charAt(n >> 2)).append(BASE64_CHARS.charAt((3 & n) << 4));
        }

        // 添加填充
        while ((result.length() & 3) > 0) {
            result.append("=");
        }

        return result.toString();
    }

    /**
     * 根据图片中的Python代码实现的getDerivedKey方法
     */
    public static byte[] getDerivedKey(String privatePem, String certificatePem) {
        try {
            // 1. 加载私钥
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            String privateKeyContent = privatePem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            // 2. 加载证书并获取公钥
            String certificateContent = certificatePem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");

            byte[] certificateBytes = Base64.getDecoder().decode(certificateContent);
            java.security.cert.CertificateFactory certFactory =
                    java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate certificate =
                    certFactory.generateCertificate(new java.io.ByteArrayInputStream(certificateBytes));
            ECPublicKey publicKey = (ECPublicKey) certificate.getPublicKey();

            // 3. 执行ECDH密钥交换
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            byte[] sharedKey = keyAgreement.generateSecret();

            // 4. 使用HKDF派生密钥
            return hkdf(sharedKey, 32);

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[32]; // 返回默认长度的空数组
        }
    }

    /**
     * HKDF实现 (基于SHA256)
     */
    private static byte[] hkdf(byte[] sharedKey, int length) {
        try {
            // HKDF-Extract
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec saltKey = new SecretKeySpec(new byte[32], "HmacSHA256"); // 空盐值
            hmac.init(saltKey);
            byte[] prk = hmac.doFinal(sharedKey);

            // HKDF-Expand
            hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec prkKey = new SecretKeySpec(prk, "HmacSHA256");
            hmac.init(prkKey);

            byte[] info = new byte[0]; // 空info
            byte[] result = new byte[length];
            byte[] t = new byte[0];

            for (int i = 1; i <= (length + 31) / 32; i++) {
                hmac.reset();
                hmac.update(t);
                hmac.update(info);
                hmac.update((byte) i);
                t = hmac.doFinal();

                int copyLength = Math.min(32, length - (i - 1) * 32);
                System.arraycopy(t, 0, result, (i - 1) * 32, copyLength);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[length];
        }
    }

    /**
     * 获取headers中的bd_ticket_guard_client_data (使用图片中的算法)
     */
    public static String getHeadersBdTicketGuardClientData(String privatePem, String certificatePem,
                                                           String tsSign, String ticket, String path, long timestamp) {
        try {
            String dataToSign = "ticket=" + ticket + "&path=" + path + "&timestamp=" + timestamp;

            // 使用图片中的算法获取派生密钥
            byte[] derivedKey = getDerivedKey(privatePem, certificatePem);

            // 使用派生密钥进行HMAC-SHA256签名
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(derivedKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] signatureBytes = hmac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));

            // 将签名转换为十六进制字符串，然后使用y函数编码
            String signatureHex = bytesToHex(signatureBytes);
            String reqSign = y(signatureHex);

            // 构建最终的JSON数据
            String jsonData = "{\"ts_sign\":\"" + tsSign + "\",\"req_content\":\"ticket,path,timestamp\"," +
                    "\"req_sign\":\"" + reqSign + "\",\"timestamp\":" + timestamp + "}";

            return Base64.getEncoder().encodeToString(jsonData.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 辅助方法
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // 测试方法
    public static void main(String[] args) {
        // 示例用法

        // 获取公钥相关数据
        String bdTicketGuardReePublicKey = getBdTicketGuardReePublicKey(publicKeyPem);
        String cookiesData = getCookiesBdTicketGuardClientData(bdTicketGuardReePublicKey);

        // 获取headers数据
        String headersData = getHeadersBdTicketGuardClientData(
                privateKeyPem, certificatePem, "ts.2.ce850aa9b2a7cd01676ce8bfe3380fbf0d9a5d8fe4062dbbd4d159745d0b3e70c4fbe87d2319cf05318624ceda14911ca406dedbebeddb2e30fce8d4fa02575d",
                "hash.c/2ZlRwpO583h8Cbh+dXXYXoOMmE+hTEakWAbbOVu2w=", "/aweme/v1/web/comment/publish", 1759032736L
        );

        System.out.println("BD Ticket Guard REE Public Key: " + bdTicketGuardReePublicKey);
        System.out.println("Cookies Data: " + cookiesData);
        System.out.println("Headers Data: " + headersData);
    }
}