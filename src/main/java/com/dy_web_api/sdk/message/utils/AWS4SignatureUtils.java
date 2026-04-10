package com.dy_web_api.sdk.message.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AWS4签名工具类 - 完整版本
 */
public class AWS4SignatureUtils {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String AWS4_REQUEST = "aws4_request";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成 x-amz-content-sha256 头部值
     * @param requestBody 请求体内容（可以是字符串、字节数组或null）
     * @return SHA256哈希值的十六进制字符串
     */
    public static String generateContentSha256(Object requestBody) {
        if (requestBody == null) {
            return generateEmptyPayloadHash();
        }

        byte[] bodyBytes;
        if (requestBody instanceof String) {
            bodyBytes = ((String) requestBody).getBytes(StandardCharsets.UTF_8);
        } else if (requestBody instanceof byte[]) {
            bodyBytes = (byte[]) requestBody;
        } else {
            // 对于其他类型，转换为字符串后编码
            bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        }

        return sha256Hex(bodyBytes);
    }

    /**
     * 生成空载荷的SHA256哈希（对于GET请求或空请求体）
     */
    public static String generateEmptyPayloadHash() {
        return sha256Hex(new byte[0]);
    }

    /**
     * 生成 "UNSIGNED-PAYLOAD" 标识（用于某些特殊场景）
     */
    public static String generateUnsignedPayload() {
        return "UNSIGNED-PAYLOAD";
    }

    /**
     * 生成签名密钥
     */
    public static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) {
        try {
            byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
            byte[] kDate = hmacSHA256(dateStamp, kSecret);
            byte[] kRegion = hmacSHA256(regionName, kDate);
            byte[] kService = hmacSHA256(serviceName, kRegion);
            byte[] kSigning = hmacSHA256(AWS4_REQUEST, kService);
            return kSigning;
        } catch (Exception e) {
            throw new RuntimeException("生成签名密钥失败", e);
        }
    }

    /**
     * 构建规范化查询字符串 - 按照AWS4标准
     */
    public static String buildCanonicalQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        // 使用TreeMap按键名字典序排序
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder canonicalQueryString = new StringBuilder();

        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (canonicalQueryString.length() > 0) {
                canonicalQueryString.append("&");
            }

            String key = entry.getKey();
            String value = entry.getValue();

            // URI编码键和值
            String encodedKey = uriEncode(key);
            String encodedValue = uriEncode(value);

            canonicalQueryString.append(encodedKey).append("=").append(encodedValue);
        }

        return canonicalQueryString.toString();
    }

    /**
     * URI编码 - 符合AWS4签名要求
     */
    private static String uriEncode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")     // 空格编码为%20而不是+
                    .replace("*", "%2A")     // *编码为%2A
                    .replace("%7E", "~");    // ~不编码
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * 构建规范化字符串 - 对应JavaScript的canonicalString
     */
    public static String buildCanonicalString(String method, String pathname, String canonicalQueryString,
                                              String canonicalHeaders, String signedHeaders, String hexEncodedBodyHash) {
        StringBuilder canonicalString = new StringBuilder();

        canonicalString.append(method.toUpperCase()).append("\n");
        canonicalString.append(pathname != null ? pathname : "/").append("\n");
        canonicalString.append(canonicalQueryString != null ? canonicalQueryString : "").append("\n");
        canonicalString.append(canonicalHeaders).append("\n");
        canonicalString.append(signedHeaders).append("\n");
        canonicalString.append(hexEncodedBodyHash);

        return canonicalString.toString();
    }

    /**
     * 构建规范化头部 - 对应JavaScript的canonicalHeaders
     */
    public static String buildCanonicalHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }

        // 按头部名称字母顺序排序
        Map<String, String> sortedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sortedHeaders.putAll(headers);

        StringBuilder canonicalHeaders = new StringBuilder();

        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            String headerName = entry.getKey().toLowerCase();
            String headerValue = entry.getValue();

            if (isSignableHeader(headerName)) {
                if (headerValue == null) {
                    throw new RuntimeException("Header " + headerName + " contains invalid value");
                }

                String canonicalizedValue = canonicalizeHeaderValue(headerValue);
                canonicalHeaders.append(headerName).append(":").append(canonicalizedValue).append("\n");
            }
        }

        return canonicalHeaders.toString();
    }

    /**
     * 规范化头部值 - 对应JavaScript的canonicalHeaderValues
     */
    private static String canonicalizeHeaderValue(String value) {
        // 替换多个连续空白字符为单个空格，并去除首尾空白
        return value.replaceAll("\\s+", " ").trim();
    }

    /**
     * 判断头部是否可签名 - 修复为静态方法
     */
    private static boolean isSignableHeader(String headerName) {
        // 这些头部通常需要签名
        return headerName.equals("x-amz-date") ||
                headerName.equals("x-amz-security-token") ||
                headerName.equals("x-amz-content-sha256") ||
                headerName.equals("host") ||
                headerName.startsWith("x-amz-");
    }

    /**
     * 获取已签名头部列表
     */
    public static String getSignedHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }

        StringBuilder signedHeaders = new StringBuilder();
        TreeMap<String, String> sortedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sortedHeaders.putAll(headers);

        for (String headerName : sortedHeaders.keySet()) {
            String lowerHeaderName = headerName.toLowerCase();
            if (isSignableHeader(lowerHeaderName)) {
                if (signedHeaders.length() > 0) {
                    signedHeaders.append(";");
                }
                signedHeaders.append(lowerHeaderName);
            }
        }

        return signedHeaders.toString();
    }

    /**
     * 构建参数字符串 - 保持原有方法兼容性
     */
    public static String buildStrParams(Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (url.length() > 0) {
                url.append("&");
            }
            url.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return url.toString();
    }

    /**
     * 生成随机字符串
     */
    public static String randomS() {
        String digits = "0123456789";
        String asciiLetters = "abcdefghigklmnopqrstuvwxyz";
        String chars = digits + asciiLetters;

        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成Authorization头 - GET请求版本
     */
    public static String generateAuthorization(String secretAccessKey, String region, String service,
                                               String canonicalQuerystring, String amzDate, String sessionToken,
                                               String dateStamp, String accessKeyID) {
        try {
            byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);

            // 构建canonical headers
            String canonicalHeaders = "x-amz-date:" + amzDate + "\n" + "x-amz-security-token:" + sessionToken + "\n";

            // 构建canonical request
            String canonicalRequest = "GET" + "\n" +
                    "/" + "\n" +
                    canonicalQuerystring + "\n" +
                    canonicalHeaders + "\n" +
                    "x-amz-date;x-amz-security-token" + "\n" +
                    sha256Hex("");

            // 构建credential scope
            String credentialScope = dateStamp + "/" + region + "/" + service + "/" + AWS4_REQUEST;

            // 构建string to sign
            String stringToSign = ALGORITHM + "\n" +
                    amzDate + "\n" +
                    credentialScope + "\n" +
                    sha256Hex(canonicalRequest);

            // 生成签名
            String signature = hmacSHA256Hex(stringToSign, signingKey);

            // 构建Authorization头
            return String.format("%s Credential=%s/%s/%s/%s/%s, SignedHeaders=x-amz-date;x-amz-security-token, Signature=%s",
                    ALGORITHM,
                    accessKeyID,
                    dateStamp,
                    region,
                    service,
                    AWS4_REQUEST,
                    signature);

        } catch (Exception e) {
            throw new RuntimeException("生成Authorization失败", e);
        }
    }

    /**
     * 生成提交上传的Authorization签名 - POST请求版本
     */
    public static String generateCommitUploadAuthorization(String secretAccessKey, String region, String service,
                                                           Map<String, String> queryParams, String amzDate,
                                                           String sessionToken, String dateStamp, String accessKeyId,
                                                           String requestBody) throws Exception {

        byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);

        // 构建规范化查询字符串
        String canonicalQueryString = buildCanonicalQueryString(queryParams);

        JsonNode jsonNode = objectMapper.readTree(requestBody);
        String jsonNodeString = jsonNode.toString();
        String contentSha256 = sha256Hex(jsonNodeString);  // 然后计算SHA256

        // 构建头部
        Map<String, String> headers = new HashMap<>();
        headers.put("x-amz-content-sha256", contentSha256);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-security-token", sessionToken);

        String canonicalHeaders = buildCanonicalHeaders(headers);
        String signedHeaders = "x-amz-content-sha256;x-amz-date;x-amz-security-token";

        // 构建规范化请求
        String canonicalRequest = "POST" + "\n" +
                "/" + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                contentSha256;

        // 调试输出
        System.out.println("=== CommitUpload 签名调试 ===");
        System.out.println("原始JSON: " + requestBody);
        System.out.println("Content SHA256: " + contentSha256);
        System.out.println("Canonical Request:\n" + canonicalRequest);

        // 构建签名字符串
        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + "aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" +
                sha256Hex(canonicalRequest);

        System.out.println("String to Sign:\n" + stringToSign);

        // 计算签名
        String signature = hmacSHA256Hex(stringToSign, signingKey);

        System.out.println("Signature: " + signature);
        System.out.println("==============================");

        // 构建Authorization头部
        return String.format("%s Credential=%s/%s/%s/%s/aws4_request, SignedHeaders=%s, Signature=%s",
                ALGORITHM, accessKeyId, dateStamp, region, service, signedHeaders, signature);
    }


    public static String generateCommentImgCommitUploadAuthorization(String secretAccessKey, String region, String service,
                                                           Map<String, String> queryParams, String amzDate,
                                                           String sessionToken, String dateStamp, String accessKeyId,
                                                           String requestBody) throws Exception {

        byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);

        // 构建规范化查询字符串
        String canonicalQueryString = buildCanonicalQueryString(queryParams);

        JsonNode jsonNode = objectMapper.readTree(requestBody);
        String jsonNodeString = jsonNode.toString();
        String contentSha256 = sha256Hex(jsonNodeString);  // 然后计算SHA256

        // 构建头部
        Map<String, String> headers = new HashMap<>();
//        headers.put("x-amz-content-sha256", contentSha256);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-security-token", sessionToken);

        String canonicalHeaders = buildCanonicalHeaders(headers);
        String signedHeaders = "x-amz-date;x-amz-security-token";

        // 构建规范化请求
        String canonicalRequest = "POST" + "\n" +
                "/" + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                contentSha256;

        // 调试输出
        System.out.println("=== CommitUpload 签名调试 ===");
        System.out.println("原始JSON: " + requestBody);
        System.out.println("Content SHA256: " + contentSha256);
        System.out.println("Canonical Request:\n" + canonicalRequest);

        // 构建签名字符串
        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + "aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" +
                sha256Hex(canonicalRequest);

        System.out.println("String to Sign:\n" + stringToSign);

        // 计算签名
        String signature = hmacSHA256Hex(stringToSign, signingKey);

        System.out.println("Signature: " + signature);
        System.out.println("==============================");

        // 构建Authorization头部
        return String.format("%s Credential=%s/%s/%s/%s/aws4_request, SignedHeaders=%s, Signature=%s",
                ALGORITHM, accessKeyId, dateStamp, region, service, signedHeaders, signature);
    }

    /**
     * 完全对应Python main方法的测试方法
     */
    public static String generateImageUploadAuthorization(String accessKeyId, String secretAccessKey,
                                                          String sessionToken, String s, String[] times) {
        String amzDate = times[0];
        String dateStamp = times[1];
        String region = "cn-north-1";
        String service = "imagex";

        Map<String, String> params = new java.util.LinkedHashMap<>(); // 保持插入顺序
        params.put("Action", "ApplyImageUpload");
        params.put("ServiceId", "jm8ajry58r");
        params.put("Version", "2018-08-01");
        params.put("app_id", "2906");
        params.put("s", s);
        params.put("user_id", "");

        String canonicalQuerystring = buildStrParams(params);

        return generateAuthorization(secretAccessKey, region, service, canonicalQuerystring,
                amzDate, sessionToken, dateStamp, accessKeyId);
    }

    private static String a(Object value) {
        if (value == null) {
            return null;
        }

        try {
            String stringValue;
            // 在JavaScript中，数组和对象都会被转换为"[object Object]"
            if (value instanceof List || value.getClass().isArray() || value instanceof Map) {
                stringValue = "[object Object]";
            } else {
                stringValue = value.toString();
            }

            // URLEncoder.encode会将空格编码为+，但我们需要%20
            String encoded = URLEncoder.encode(stringValue, StandardCharsets.UTF_8);
            // 将+替换为%20（只替换空格对应的+，不影响其他+）
            return encoded.replace("+", "%20");

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Java版本的s()函数 - 精确复制JavaScript逻辑
     * 修正版：正确处理数组情况
     */
    public static String s(Map<String, Object> e) {
        return e.keySet().stream()
                .sorted()  // Object.keys(e).sort()
                .map(t -> {  // .map(function(t) {
                    Object r = e.get(t);  // var r = e[t];
                    String encodedKey = a(t);  // 先编码键
                    if (r != null && encodedKey != null) {  // if (null != r && (t = a(t)))
                        if (r instanceof List || r.getClass().isArray()) {  // Array.isArray(r)
                            // 对于数组，直接返回"[object Object]"
                            return encodedKey + "=" + a(r);  // 这里a(r)会返回编码后的"[object Object]"
                        } else {
                            return encodedKey + "=" + a(r);  // "".concat(t, "=").concat(a(r))
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)  // .filter(function(e) { return e })
                .collect(Collectors.joining("&"));  // .join("&")
    }

    /**
     * 将JSON字符串转换为Map，然后应用s()函数
     */
    public static String convertJsonToUrlEncoded(String jsonString) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        Map<String, Object> map = jsonNodeToMap(jsonNode);
        return s(map);
    }

    /**
     * 将JsonNode转换为Map
     */
    public static Map<String, Object> jsonNodeToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                map.put(field.getKey(), jsonNodeToValue(field.getValue()));
            }
        }

        return map;
    }

    /**
     * 将JsonNode转换为相应的Java对象
     */
    private static Object jsonNodeToValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(jsonNodeToValue(item));
            }
            return list;
        } else if (node.isObject()) {
            return jsonNodeToMap(node);
        } else {
            return node.toString();
        }
    }

    /**
     * HMAC-SHA256加密
     */
    private static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HMAC-SHA256加密并返回十六进制字符串
     */
    public static String hmacSHA256Hex(String data, byte[] key) throws Exception {
        return bytesToHex(hmacSHA256(data, key));
    }

    /**
     * SHA256哈希 - 字符串版本
     */
    public static String sha256Hex(String data) {
        return sha256Hex(data.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * SHA256哈希 - 字节数组版本
     */
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA256加密失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 获取当前UTC时间 - 返回[amzDate, dateStamp]
     */
    public static String[] getCurrentUTCTime() {
        ZonedDateTime utcTime = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = utcTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String dateStamp = utcTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return new String[]{amzDate, dateStamp};
    }

    // 移除重复的方法定义，保留convertJsonToUrlEncoded作为主要实现

    /**
     * 生成用于CommitUpload的Content SHA256
     */
    public static String generateCommitUploadContentSha256(String jsonBody) throws Exception {
        String urlEncodedBody = convertJsonToUrlEncoded(jsonBody);
        return sha256Hex(urlEncodedBody);
    }


    public static String generateCommitUploadAuthorization2(String secretAccessKey, String region, String service,
                                                           Map<String, String> queryParams, String amzDate,
                                                           String sessionToken, String dateStamp, String accessKeyId,
                                                           String requestBody) throws Exception {

        byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);

        // 构建规范化查询字符串
        String canonicalQueryString = buildCanonicalQueryString(queryParams);

        JsonNode jsonNode = objectMapper.readTree(requestBody);
        String jsonNodeString = jsonNode.toString();
        String contentSha256 = sha256Hex(jsonNodeString);  // 然后计算SHA256

        // 构建头部
        Map<String, String> headers = new HashMap<>();
        headers.put("x-amz-content-sha256", contentSha256);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-security-token", sessionToken);

        String canonicalHeaders = buildCanonicalHeaders(headers);
        String signedHeaders = "x-amz-content-sha256;x-amz-date;x-amz-security-token";

        // 构建规范化请求
        String canonicalRequest = "POST" + "\n" +
                "/" + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                contentSha256;

        // 调试输出
        System.out.println("=== CommitUpload 签名调试 (修正版) ===");
        System.out.println("原始JSON: " + requestBody);
        System.out.println("Content SHA256: " + contentSha256);
        System.out.println("期望SHA256: 7249c413da45fe060009f02cdaf0d168a0fb09cac5f1967e657cfaa685d3dc8a");
        System.out.println("SHA256匹配: " + "7249c413da45fe060009f02cdaf0d168a0fb09cac5f1967e657cfaa685d3dc8a".equals(contentSha256));
        System.out.println("Canonical Request:\n" + canonicalRequest);

        // 构建签名字符串
        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + "aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" +
                sha256Hex(canonicalRequest);

        System.out.println("String to Sign:\n" + stringToSign);

        // 计算签名
        String signature = hmacSHA256Hex(stringToSign, signingKey);

        System.out.println("Signature: " + signature);
        System.out.println("==============================");

        // 构建Authorization头部
        return String.format("%s Credential=%s/%s/%s/%s/aws4_request, SignedHeaders=%s, Signature=%s",
                ALGORITHM, accessKeyId, dateStamp, region, service, signedHeaders, signature);
    }
}