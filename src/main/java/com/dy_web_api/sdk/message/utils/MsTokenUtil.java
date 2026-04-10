package com.dy_web_api.sdk.message.utils;

import com.dy_web_api.sdk.message.config.TokenConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(MsTokenUtil.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // 私有构造函数，防止实例化
    private MsTokenUtil() {
    }

    /**
     * 生成真实的msToken，当出现错误时返回虚假的值
     * Generate a real msToken and return a false value when an error occurs
     */
    public static String genRealMsToken(TokenConfig tokenConfig) {
        try {
            // 构建请求载荷
            Map<String, Object> payload = new HashMap<>();
            payload.put("magic", tokenConfig.getMagic());
            payload.put("version", tokenConfig.getVersion());
            payload.put("dataType", tokenConfig.getDataType());
            payload.put("strData", tokenConfig.getStrData());
            payload.put("tspFromClient", getCurrentTimestamp());

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenConfig.getUrl()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("User-Agent", tokenConfig.getUserAgent())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // 执行请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Unexpected response code: " + response.statusCode());
            }

            // 从响应cookies中提取msToken
            String msToken = extractMsTokenFromCookies(response);

            if (msToken != null && !msToken.isEmpty()) {
                logger.info("成功生成真实的msToken");
                return msToken;
            } else {
                throw new RuntimeException("未能从响应中获取到msToken");
            }

        } catch (Exception e) {
            // 返回虚假的msToken
            logger.error("生成TikTok msToken API错误：{}", e.getMessage());
            logger.info("当前网络无法正常访问TikTok服务器，已经使用虚假msToken以继续运行。");
            logger.info("并且TikTok相关API大概率无法正常使用，请在配置中更新代理。");
            logger.info("如果你不需要使用TikTok相关API，请忽略此消息。");
            return genFalseMsToken();
        }
    }

    /**
     * 从响应cookies中提取msToken
     */
    private static String extractMsTokenFromCookies(HttpResponse<String> response) {
        List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");

        for (String cookieHeader : setCookieHeaders) {
            if (cookieHeader.contains("msToken=")) {
                String[] parts = cookieHeader.split(";");
                for (String part : parts) {
                    if (part.trim().startsWith("msToken=")) {
                        return part.trim().substring("msToken=".length());
                    }
                }
            }
        }
        return null;
    }

    /**
     * 生成虚假的msToken
     */
    public static String genFalseMsToken() {
        // 生成一个类似真实msToken格式的假token
        return generateRandomToken(88) + "=";
    }

    /**
     * 生成随机token字符串
     */
    private static String generateRandomToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }



    public static void main(String[] args) {
        TokenConfig config = TokenConfig.createFromYourConfig();
        String msToken = MsTokenUtil.genRealMsToken(config);
        System.out.println("Generated msToken: " + msToken);
    }
}