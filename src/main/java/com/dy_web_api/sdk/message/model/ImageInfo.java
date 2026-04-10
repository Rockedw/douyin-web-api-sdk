package com.dy_web_api.sdk.message.model;

import lombok.Builder;
import lombok.Data;

/**
 * 图片信息模型
 */
@Data
@Builder(toBuilder = true)
public class ImageInfo {
    private int width;
    private int height;
    private int fileSize;
    private String md5;
    private String skey;
    private String uri;
}