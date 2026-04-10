package com.dy_web_api.sdk.message.service;

import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.handler.ResourceUploader;
import com.dy_web_api.sdk.message.model.ImageInfo;
import com.dy_web_api.sdk.message.utils.AWS4SignatureGenerator;
import com.dy_web_api.sdk.message.utils.AWS4SignatureUtils;
import com.dy_web_api.sdk.message.utils.ImageUploadAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * 图片上传服务
 */
@Slf4j
@RequiredArgsConstructor
public class ImageUploadService {

    private enum UploadType {
        MESSAGE,
        COMMENT
    }
    
    private final DouyinConfig config;
    private final ResourceUploader resourceUploader;
    private ImageInfo upload(String imagePath, byte[] imageData, String fileName, UploadType uploadType) {
        try {
            ImageInfo imageInfo;
            if (imagePath != null) {
                File file = new File(imagePath);
                if (!file.exists() || !file.isFile()) {
                    throw new DouyinMessageException(ErrorCode.FILE_NOT_FOUND, "文件不存在或不是文件: " + imagePath);
                }
                BufferedImage image = ImageIO.read(file);
                imageInfo = ImageInfo.builder()
                        .width(image != null ? image.getWidth() : 800)
                        .height(image != null ? image.getHeight() : 600)
                        .fileSize((int) file.length())
                        .build();
            } else {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                imageInfo = ImageInfo.builder()
                        .width(image != null ? image.getWidth() : 800)
                        .height(image != null ? image.getHeight() : 600)
                        .fileSize(imageData.length)
                        .build();
            }

            return performUnifiedUpload(imageInfo, imagePath, imageData, uploadType);
        } catch (Exception e) {
            log.error("上传图片失败: {}", (imagePath != null ? imagePath : fileName), e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "上传图片失败", e);
        }
    }

    public ImageInfo uploadImage(String imagePath) {
        return upload(imagePath, null, imagePath, UploadType.MESSAGE);
    }
    
    public ImageInfo uploadCommentImage(String imagePath) {
        return upload(imagePath, null, imagePath, UploadType.COMMENT);
    }

    public ImageInfo uploadImage(byte[] imageData, String fileName) {
        return upload(null, imageData, fileName, UploadType.MESSAGE);
    }
    
    public ImageInfo uploadCommentImage(byte[] imageData, String fileName) {
        return upload(null, imageData, fileName, UploadType.COMMENT);
    }
    
    private ImageInfo buildImageInfo(ImageInfo imageInfo, ResourceUploader.CommitUploadResponse commitResponse, UploadType uploadType) {
        if (uploadType == UploadType.COMMENT) {
            if (commitResponse.getResults() == null || commitResponse.getResults().isEmpty()) {
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "提交上传失败，返回结果为空");
            }
            ResourceUploader.CommitUploadResponse.UploadResult result = commitResponse.getResults().get(0);
            ResourceUploader.CommitUploadResponse.PluginResult pluginResult = result.getPluginResult();

            return imageInfo.toBuilder()
                    .uri(pluginResult != null ? pluginResult.getImageUri() : result.getUri())
                    .md5(pluginResult != null ? pluginResult.getImageMd5() : null)
                    .width(pluginResult != null ? pluginResult.getImageWidth() : imageInfo.getWidth())
                    .height(pluginResult != null ? pluginResult.getImageHeight() : imageInfo.getHeight())
                    .fileSize(pluginResult != null ? pluginResult.getImageSize() : imageInfo.getFileSize())
                    .build();
        } else {
            return imageInfo.toBuilder()
                    .uri(commitResponse.getFirstEncryptionUri())
                    .md5(commitResponse.getResults().get(0).getEncryption().getSourceMd5())
                    .skey(commitResponse.getFirstSecretKey())
                    .build();
        }
    }

    private void uploadFile(ResourceUploader.UploadAddressResponse uploadAddress, String imagePath, byte[] imageData) {
        ResourceUploader.UploadFileResponse uploadFileResponse;
        if (imagePath != null) {
            uploadFileResponse = resourceUploader.uploadFile(uploadAddress, imagePath);
        } else {
            uploadFileResponse = resourceUploader.uploadFileToTos(uploadAddress, imageData);
        }

        if (!uploadFileResponse.isSuccess()) {
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "文件上传失败: " + uploadFileResponse.getMessage());
        }
        log.debug("文件上传成功");
    }

    private ImageInfo performUnifiedUpload(ImageInfo imageInfo, String imagePath, byte[] imageData, UploadType uploadType) {
        try {
            String[] times = AWS4SignatureGenerator.getCurrentUTCTime();
            String s = AWS4SignatureUtils.randomS();
            String fileSize = String.valueOf(imageInfo.getFileSize());

            ResourceUploader.UploadAddressResponse uploadAddress;
            ResourceUploader.CommitUploadResponse commitResponse;

            if (uploadType == UploadType.COMMENT) {
                // 评论图片上传流程
                ResourceUploader.CommentImageUploadAuthResponse auth = resourceUploader.uploadCommentImgAuth();
                log.debug("获取评论图片上传授权成功");

                String authorization = ImageUploadAuthUtil.generateCommentImageUploadAuthorization(
                        auth.getAccessKey(), auth.getSecretKey(), auth.getSessionToken(), "", s, fileSize, times);

                uploadAddress = resourceUploader.getCommentImgResourceUploadAddress(
                        authorization, auth.getSessionToken(), s, fileSize, "", times[0]);
                log.debug("获取评论图片上传地址成功");

                uploadFile(uploadAddress, imagePath, imageData);

                String sessionKey = uploadAddress.getInnerUploadAddress().getSessionKey();
                commitResponse = resourceUploader.commitCommentImgUpload(
                        sessionKey, auth.getAccessKey(), auth.getSecretKey(), auth.getSessionToken(), times);
            } else {
                // 私信图片上传流程
                ResourceUploader.UploadAuthResponse auth = resourceUploader.uploadAuth();
                log.debug("获取私信图片上传授权成功");

                String authorization = ImageUploadAuthUtil.generateImageUploadAuthorization(
                        auth.getAccessKeyId(), auth.getSecretAccessKey(), auth.getSessionToken(), "", s, fileSize, times);

                uploadAddress = resourceUploader.getResourceUploadAddress(
                        authorization, auth.getSessionToken(), s, fileSize, "", times[0]);
                log.debug("获取私信图片上传地址成功");

                uploadFile(uploadAddress, imagePath, imageData);

                String sessionKey = uploadAddress.getInnerUploadAddress().getSessionKey();
                commitResponse = resourceUploader.commitUpload(
                        sessionKey, auth.getAccessKeyId(), auth.getSecretAccessKey(), auth.getSessionToken(), times);
            }

            if (commitResponse == null || !commitResponse.isSuccess()) {
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "提交上传失败");
            }
            log.debug("提交上传成功");

            return buildImageInfo(imageInfo, commitResponse, uploadType);

        } catch (Exception e) {
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "图片上传过程失败", e);
        }
    }
}