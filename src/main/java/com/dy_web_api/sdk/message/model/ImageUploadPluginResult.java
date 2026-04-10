package com.dy_web_api.sdk.message.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图片上传结果中的PluginResult对象
 */
public class ImageUploadPluginResult {

    /**
     * 文件名
     */
    @JsonProperty("FileName")
    private String fileName;

    /**
     * 源URI地址
     */
    @JsonProperty("SourceUri")
    private String sourceUri;

    /**
     * 图片URI地址
     */
    @JsonProperty("ImageUri")
    private String imageUri;

    /**
     * 图片宽度（像素）
     */
    @JsonProperty("ImageWidth")
    private Integer imageWidth;

    /**
     * 图片高度（像素）
     */
    @JsonProperty("ImageHeight")
    private Integer imageHeight;

    /**
     * 图片MD5哈希值
     */
    @JsonProperty("ImageMd5")
    private String imageMd5;

    /**
     * 图片格式（如：webp, jpg, png等）
     */
    @JsonProperty("ImageFormat")
    private String imageFormat;

    /**
     * 图片文件大小（字节）
     */
    @JsonProperty("ImageSize")
    private Long imageSize;

    /**
     * 帧数（动图时大于1，静态图为1）
     */
    @JsonProperty("FrameCnt")
    private Integer frameCnt;

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getImageMd5() {
        return imageMd5;
    }

    public void setImageMd5(String imageMd5) {
        this.imageMd5 = imageMd5;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public Long getImageSize() {
        return imageSize;
    }

    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }

    public Integer getFrameCnt() {
        return frameCnt;
    }

    public void setFrameCnt(Integer frameCnt) {
        this.frameCnt = frameCnt;
    }

    @Override
    public String toString() {
        return "ImageUploadPluginResult{" +
                "fileName='" + fileName + '\'' +
                ", sourceUri='" + sourceUri + '\'' +
                ", imageUri='" + imageUri + '\'' +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", imageMd5='" + imageMd5 + '\'' +
                ", imageFormat='" + imageFormat + '\'' +
                ", imageSize=" + imageSize +
                ", frameCnt=" + frameCnt +
                '}';
    }
}
