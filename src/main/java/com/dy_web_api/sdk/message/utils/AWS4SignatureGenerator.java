package com.dy_web_api.sdk.message.utils;

import java.util.Map;

/**
 * AWS4签名生成器 - 简化版本，委托给AWS4SignatureUtils
 */
public class AWS4SignatureGenerator {

    /**
     * 构建规范化查询字符串
     */
    public static String buildCanonicalQueryString(Map<String, String> params) {
        return AWS4SignatureUtils.buildCanonicalQueryString(params);
    }

    /**
     * 构建参数字符串
     */
    public static String buildStrParams(Map<String, String> params) {
        return AWS4SignatureUtils.buildStrParams(params);
    }

    /**
     * 生成随机字符串
     */
    public static String randomS() {
        return AWS4SignatureUtils.randomS();
    }

    /**
     * 生成Authorization头
     */
    public static String generateAuthorization(String secretAccessKey, String region, String service,
                                               String canonicalQuerystring, String amzDate, String sessionToken,
                                               String dateStamp, String accessKeyID) {
        return AWS4SignatureUtils.generateAuthorization(secretAccessKey, region, service,
                canonicalQuerystring, amzDate, sessionToken, dateStamp, accessKeyID);
    }

    /**
     * 获取当前UTC时间
     */
    public static String[] getCurrentUTCTime() {
        return AWS4SignatureUtils.getCurrentUTCTime();
    }
}