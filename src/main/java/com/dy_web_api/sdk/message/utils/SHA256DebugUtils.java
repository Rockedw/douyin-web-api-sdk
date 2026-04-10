package com.dy_web_api.sdk.message.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SHA256DebugUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void debugSHA256Calculation(String input) {
        System.out.println("=== SHA256 调试信息 ===");
        System.out.println("输入字符串: " + input);
        System.out.println("字符串长度: " + input.length());

        // 显示字符串的字节表示
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        System.out.println("UTF-8字节长度: " + bytes.length);

        // 显示前100个字节的十六进制表示
        StringBuilder hexStr = new StringBuilder();
        for (int i = 0; i < Math.min(100, bytes.length); i++) {
            hexStr.append(String.format("%02x ", bytes[i] & 0xFF));
            if ((i + 1) % 16 == 0) hexStr.append("\n");
        }
        System.out.println("前100字节(hex):\n" + hexStr.toString());

        // 计算SHA256
        String sha256 = AWS4SignatureUtils.sha256Hex(input);
        System.out.println("计算的SHA256: " + sha256);

        // 期望的SHA256
        String expectedSha256 = "7249c413da45fe060009f02cdaf0d168a0fb09cac5f1967e657cfaa685d3dc8a";
        System.out.println("期望的SHA256: " + expectedSha256);
        System.out.println("SHA256匹配: " + expectedSha256.equals(sha256));

        // 如果不匹配，尝试不同的编码方式
        if (!expectedSha256.equals(sha256)) {
            System.out.println("\n=== 尝试其他编码方式 ===");

            // 尝试直接使用字节数组
            String sha256Direct = AWS4SignatureUtils.sha256Hex(bytes);
            System.out.println("直接字节数组SHA256: " + sha256Direct);

            // 尝试ISO-8859-1编码
            byte[] iso88591Bytes = input.getBytes(StandardCharsets.ISO_8859_1);
            String sha256ISO = AWS4SignatureUtils.sha256Hex(iso88591Bytes);
            System.out.println("ISO-8859-1编码SHA256: " + sha256ISO);

            // 尝试ASCII编码
            byte[] asciiBytes = input.getBytes(StandardCharsets.US_ASCII);
            String sha256ASCII = AWS4SignatureUtils.sha256Hex(asciiBytes);
            System.out.println("ASCII编码SHA256: " + sha256ASCII);
        }

        System.out.println("=====================");
    }



    /**
     * 将Map转换为签名字符串并计算SHA256哈希
     * @param requestData 输入的Map数据
     * @return 十六进制编码的哈希字符串
     */
    public static String hexEncodedBodyHash(Map<String, Object> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return hexEncodedHash("");
        }

        String serializedData = serializeMap(requestData);
        return hexEncodedHash(serializedData);
    }

    /**
     * 计算字符串的SHA256哈希值
     * @param input 输入字符串
     * @return 十六进制编码的哈希字符串
     */
    public static String hexEncodedHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 将Map序列化为字符串（类似JavaScript的s函数）
     * @param map 输入的Map
     * @return 序列化后的字符串
     */
    private static String serializeMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // 按key排序
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value == null) {
                        return null;
                    }

                    String encodedKey = encodeComponent(key);
                    if (encodedKey == null || encodedKey.isEmpty()) {
                        return null;
                    }

                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        String encodedValues = list.stream()
                                .map(item -> encodeComponent(String.valueOf(item)))
                                .filter(Objects::nonNull)
                                .sorted()
                                .collect(Collectors.joining("&" + encodedKey + "="));

                        return encodedKey + "=" + encodedValues;
                    } else {
                        String encodedValue = encodeComponent(String.valueOf(value));
                        return encodedValue != null ? encodedKey + "=" + encodedValue : null;
                    }
                })
                .filter(Objects::nonNull) // 过滤掉null值
                .collect(Collectors.joining("&"));
    }

    /**
     * URL编码组件（类似JavaScript的a函数）
     * @param component 需要编码的字符串
     * @return 编码后的字符串
     */
    private static String encodeComponent(String component) {
        if (component == null) {
            return null;
        }

        try {
            // 使用URLEncoder进行编码
            String encoded = URLEncoder.encode(component, StandardCharsets.UTF_8.name());

            // 根据JavaScript代码的逻辑，替换特殊字符
            // 保留 A-Za-z0-9_.~-%，其他字符进行特殊处理
            encoded = encoded.replace("+", "%20"); // URLEncoder将空格编码为+，但我们需要%20

            // 处理*字符的特殊编码
            encoded = encoded.replace("*", "%2A");

            return encoded;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static void main(String[] args) throws JsonProcessingException {
        // 使用你提供的JSON（确保SessionKey是正确的）
        String jsonBody = "{\n" +
                "    \"SessionKey\": \"eyJhY2NvdW50VHlwZSI6InNwYWNlIiwiYXBwSWQiOiIiLCJiaXpUeXBlIjoiIiwiZmlsZVR5cGUiOiJpbWFnZSIsImxlZ2FsIjoiIiwic3RvcmVJbmZvcyI6Ilt7XCJTdG9yZVVyaVwiOlwidG9zLWNuLW8tMDAwNjEvb29BQTB6b0FRaUltRHBJQ2FhQkZJanZpRjk2TUFLdnpBaXdFZ1wiLFwiQXV0aFwiOlwiU3BhY2VLZXkvemhlbnpoZW4vMS86dmVyc2lvbjp2MjpleUpoYkdjaU9pSklVekkxTmlJc0luUjVjQ0k2SWtwWFZDSjkuZXlKbGVIQWlPakUzTlRjMk1EYzJNallzSW5OcFoyNWhkSFZ5WlVsdVptOGlPbnNpWVdOalpYTnpTMlY1SWpvaVptRnJaVjloWTJObGMzTmZhMlY1SWl3aVluVmphMlYwSWpvaWRHOXpMV051TFc4dE1EQXdOakVpTENKbGVIQnBjbVVpT2pFM05UYzJNRGMyTWpZc0ltWnBiR1ZKYm1admN5STZXM3NpYjJsa1MyVjVJam9pYjI5QlFUQjZiMEZSYVVsdFJIQkpRMkZoUWtaSmFuWnBSamsyVFVGTGRucEJhWGRGWnlJc0ltWnBiR1ZVZVhCbElqb2lNU0o5WFN3aVpYaDBjbUVpT25zaVlXTmpiM1Z1ZEY5d2NtOWtkV04wSWpvaWRtOWtJaXdpWW14dlkydGZiVzlrWlNJNklpSXNJbU52Ym5SbGJuUmZkSGx3WlY5aWJHOWpheUk2SW50Y0ltMXBiV1ZmY0dOMFhDSTZNQ3hjSW0xdlpHVmNJam93TEZ3aWJXbHRaVjlzYVhOMFhDSTZiblZzYkN4Y0ltTnZibVpzYVdOMFgySnNiMk5yWENJNlptRnNjMlY5SWl3aVpXNWpjbmx3ZEY5aGJHZHZJam9pSWl3aVpXNWpjbmx3ZEY5clpYa2lPaUlpTENKbWIzSmlhV1JmWkhWd1gzUnZjMTlyWlhraU9uUnlkV1VzSW5Od1lXTmxJam9pZW1obGJucG9aVzRpTENKMGIzTmZiV1YwWVNJNkludGNJbFZUUlZKZlNVUmNJanBjSWpnNE1qRTBORFUwTWpBNFhDSjlJbjE5ZlEuUmJLNTQ0aTBDazVtcUhlbldESU1PSm9pclhhVFNhd3p0UzBaOEdwUWUzMFwiLFwiVXBsb2FkSURcIjpcIjE4OGE2OTQyNGNlYTQyZTQ4OTA3NjNhNWU5YWRiNmM3XCIsXCJVcGxvYWRIZWFkZXJcIjpudWxsLFwiU3RvcmFnZUhlYWRlclwiOm51bGx9XSIsInVwbG9hZEhvc3QiOiJ0b3MtZC1jbS1sZi5kb3V5aW4uY29tIiwidXJpIjoidG9zLWNuLW8tMDAwNjEvb29BQTB6b0FRaUltRHBJQ2FhQkZJanZpRjk2TUFLdnpBaXdFZyIsInVzZXJJZCI6IiJ9\",\n" +
                "    \"Functions\": [\n" +
                "        {\n" +
                "            \"name\": \"Encryption\",\n" +
                "            \"input\": {\n" +
                "                \"Config\": {\n" +
                "                    \"copies\": \"cipher_v2\"\n" +
                "                },\n" +
                "                \"PolicyParams\": {\n" +
                "                    \"policy-set\": \"check,thumb,medium,large\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}"
                ;

        // 期望的SHA256
        String expectedSHA256 = "7249c413da45fe060009f02cdaf0d168a0fb09cac5f1967e657cfaa685d3dc8a";

        // 测试修正后的逻辑
        System.out.println("=== 测试修正后的SHA256计算 ===");

        // 1. 解析JSON为Map
        JsonNode jsonNode = objectMapper.readTree(jsonBody);

        // 3. 计算SHA256
        String actualSHA256 = AWS4SignatureUtils.sha256Hex(jsonNode.toString());
//        String actualSHA256 = hexEncodedBodyHash(requestBodyMap);
        System.out.println("计算的SHA256: " + actualSHA256);
        System.out.println("期望的SHA256: " + expectedSHA256);
        System.out.println("SHA256匹配: " + expectedSHA256.equals(actualSHA256));

        // 如果还是不匹配，我们需要检查SessionKey是否正确
        if (!expectedSHA256.equals(actualSHA256)) {
            System.out.println("\n=== SessionKey检查 ===");
            String sessionKey = jsonNode.get("SessionKey").asText();
            System.out.println("当前SessionKey长度: " + sessionKey.length());

            // 尝试使用另一个SessionKey（如果你有的话）
            // 这里我们可能需要使用与期望SHA256匹配的正确SessionKey
        }
    }
}