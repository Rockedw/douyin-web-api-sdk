package com.dy_web_api.sdk.message.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 错误码枚举
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 连接相关错误
    CONNECTION_FAILED("CONN_001", "连接失败"),
    CONNECTION_TIMEOUT("CONN_002", "连接超时"),
    CONNECTION_INTERRUPTED("CONN_003", "连接中断"),
    
    // 配置相关错误
    CONFIG_INVALID("CFG_001", "配置无效"),
    CONFIG_MISSING("CFG_002", "配置缺失"),
    
    // 消息相关错误
    MESSAGE_SEND_FAILED("MSG_001", "消息发送失败"),
    MESSAGE_FORMAT_ERROR("MSG_002", "消息格式错误"),
    CONVERSATION_CREATE_FAILED("MSG_003", "会话创建失败"),
    
    // 文件上传相关错误
    FILE_NOT_FOUND("FILE_001", "文件不存在"),
    FILE_TOO_LARGE("FILE_002", "文件过大"),
    FILE_FORMAT_UNSUPPORTED("FILE_003", "文件格式不支持"),
    UPLOAD_AUTH_FAILED("FILE_004", "上传授权失败"),
    UPLOAD_FAILED("FILE_005", "上传失败"),
    
    // 网络相关错误
    NETWORK_ERROR("NET_001", "网络错误"),
    HTTP_ERROR("NET_002", "HTTP请求错误"),
    
    // 系统相关错误
    SYSTEM_ERROR("SYS_001", "系统错误"),
    THREAD_INTERRUPTED("SYS_002", "线程中断"),
    
    // 认证相关错误
    AUTH_FAILED("AUTH_001", "认证失败"),
    SESSION_EXPIRED("AUTH_002", "会话已过期"),
    ACCESS_DENIED("AUTH_003", "访问被拒绝"),
    
    // 参数相关错误
    INVALID_PARAMETER("PARAM_001", "参数无效"),
    MISSING_PARAMETER("PARAM_002", "缺少必要参数"),
    
    // 业务相关错误
    CONVERSATION_NOT_FOUND("BIZ_001", "会话不存在"),
    MESSAGE_TOO_LONG("BIZ_002", "消息内容过长"),
    RATE_LIMIT_EXCEEDED("BIZ_003", "请求频率超限"),
    
    // 服务相关错误
    SERVICE_UNAVAILABLE("SVC_001", "服务不可用"),
    TIMEOUT("SVC_002", "请求超时"),
    UNKNOWN_ERROR("SVC_003", "未知错误"),
    
    // 评论相关错误
    COMMENT_FAILED("COMMENT_001", "评论发布失败"),
    VIDEO_NOT_FOUND("COMMENT_002", "视频不存在"),
    RESPONSE_FORMAT_ERROR("COMMENT_003", "响应格式错误"),
    RESPONSE_PARSE_ERROR("COMMENT_004", "响应解析错误"),

    API_ERROR("API_001", "API调用失败"),
    PARSE_ERROR("API_002", "解析错误");



    private final String code;
    private final String message;
}