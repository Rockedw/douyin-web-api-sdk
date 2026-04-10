package com.dy_web_api.sdk.message.utils;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP工具类
 */
public class HttpUtils {

    /**
     * 构建带参数的URL
     */
    public static String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            url.append("?");
        } else if (!baseUrl.endsWith("&")) {
            url.append("&");
        }

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : params.entrySet()) {
            String encodeValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            sb.append(entry.getKey()).append("=").append(encodeValue).append("&");
        }
        if(! sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        url.append(sb);


        return url.toString();
    }

    public static String params2Str(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : params.entrySet()) {
            String encodeValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            sb.append(entry.getKey()).append("=").append(encodeValue).append("&");
        }
        if(! sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String buildRequestUrl(Map<String, String> params, String url, String userAgent) {
        StringBuilder sb = new StringBuilder(url);
        String params2Str = HttpUtils.params2Str(params);
        String aBogus = ABogusUtil.generateABogus(params2Str, userAgent);
        sb.append(params2Str + "&a_bogus=" + URLEncoder.encode(aBogus, StandardCharsets.UTF_8));
        return sb.toString();
    }

    /**
     * 参数Map转字符串
     */
    public static String paramsToString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        params.entrySet().forEach(entry -> {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        });

        return sb.toString();
    }
}