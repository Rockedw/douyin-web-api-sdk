package com.dy_web_api.sdk.message.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片上传认证工具类
 */
public class ImageUploadAuthUtil {

    /**
     * 生成图片上传Authorization
     */
    public static String generateImageUploadAuthorization(String accessKeyId, String secretAccessKey, String sessionToken, String userId, String s, String fileSize, String[] times) {
        // 获取当前UTC时间
        String amzDate = times[0];
        String dateStamp = times[1];

        String region = "cn-north-1";
        String service = "vod";

        Map<String, String> params = new HashMap<>();
        params.put("Action", "ApplyUploadInner");
        params.put("FileSize", fileSize);
        params.put("FileType", "image");
        params.put("IsInner", "1");
        params.put("s", s);
        params.put("SpaceName", "zhenzhen");
        params.put("Version", "2020-11-19");
        params.put("NeedFallback", "true");

        String canonicalQuerystring = AWS4SignatureGenerator.buildCanonicalQueryString(params);

        return AWS4SignatureGenerator.generateAuthorization(
                secretAccessKey,
                region,
                service,
                canonicalQuerystring,
                amzDate,
                sessionToken,
                dateStamp,
                accessKeyId
        );
    }


    public static String generateCommentImageUploadAuthorization(String accessKeyId, String secretAccessKey, String sessionToken, String userId, String s, String fileSize, String[] times) {
        // 获取当前UTC时间
        String amzDate = times[0];
        String dateStamp = times[1];

        String region = "cn-north-1";
        String service = "imagex";

        Map<String, String> params = new HashMap<>();
        params.put("Action", "ApplyImageUpload");
        params.put("s", s);
        params.put("Version", "2018-08-01");
        params.put("ServiceId", "p14lwwcsbr");

        String canonicalQuerystring = AWS4SignatureGenerator.buildCanonicalQueryString(params);

        return AWS4SignatureGenerator.generateAuthorization(
                secretAccessKey,
                region,
                service,
                canonicalQuerystring,
                amzDate,
                sessionToken,
                dateStamp,
                accessKeyId
        );
    }




    /**
     * 生成完整的图片上传请求URL
     */
    public static String generateImageUploadUrl(String accessKeyId, String secretAccessKey, String sessionToken, String userId) {
        // 获取当前UTC时间
        String[] times = AWS4SignatureGenerator.getCurrentUTCTime();
        String amzDate = times[0];

        // 构建参数
        Map<String, String> params = new HashMap<>();
        params.put("Action", "ApplyImageUpload");
        params.put("ServiceId", "p14lwwcsbr");
        params.put("Version", "2018-08-01");
        params.put("app_id", "2906");
        params.put("s", AWS4SignatureGenerator.randomS());
        params.put("user_id", userId != null ? userId : "");

        String queryString = AWS4SignatureGenerator.buildStrParams(params);

        return "https://imagex.volcengineapi.com/?" + queryString;
    }
}