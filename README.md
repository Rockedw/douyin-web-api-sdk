# 抖音api能力合集 (有用的话请给一个star⭐)

一个基于 Java 的抖音 Web 能力 SDK，支持私信收发、视频评论、图片上传、用户信息获取、用户作品抓取与 WebSocket 实时消息接收。

2026-04-10 可用

## 项目定位

本项目聚焦于抖音 Web 侧常见能力的工程化封装，目标是提供一个相对统一的 Java SDK 入口，方便在业务侧接入以下能力：

- 私信发送
- 图片消息发送
- 视频卡片消息发送
- WebSocket 实时接收消息
- 陌生人会话拉取
- 用户信息获取
- 用户作品列表抓取
- 视频评论发布
- 视频评论列表获取

当前统一入口类为 `DouyinMessageSDK`。

## 功能特性

- 基于 `CompletableFuture` 的异步消息发送接口
- WebSocket 长连接、心跳、超时检测与自动重连
- 图片上传与消息发送、图片上传与视频评论链路封装
- 用户信息、用户作品、评论列表等 Web 接口能力封装
- 基于 protobuf 的消息构建与解析
- 统一异常模型与错误码定义

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 1. 获取源码

```bash
git clone git@github.com:Rockedw/douyin-web-api-sdk.git
cd douyin-web-api-sdk
```

### 2. 编译项目

```bash
mvn clean compile
```

### 3. 基础使用

```java
import com.dy_web_api.sdk.message.DouyinMessageSDK;
import com.dy_web_api.sdk.message.config.DouyinConfig;

DouyinConfig config = DouyinConfig.builder()
    .sessionId("your_session_id")
    .userId("your_user_id")
    .build();

try (DouyinMessageSDK sdk = new DouyinMessageSDK(config)) {
    sdk.sendMessage("conversation_id", 123456789L, "Hello Douyin", false).get();
}
```

## 详细用法

### 1. 初始化 SDK

最小初始化只需要 `sessionId` 和 `userId`：

```java
import com.dy_web_api.sdk.message.DouyinMessageSDK;
import com.dy_web_api.sdk.message.config.DouyinConfig;

DouyinConfig config = DouyinConfig.builder()
    .sessionId("your_session_id")
    .userId("your_user_id")
    .build();

DouyinMessageSDK sdk = new DouyinMessageSDK(config);
```

如果你要同时使用消息接收、用户作品、上传、评论等更多能力，建议把常用参数一起补齐：

```java
DouyinConfig config = DouyinConfig.builder()
    .sessionId("your_session_id")
    .userId("your_user_id")
    .accessKey("your_access_key")
    .deviceId("your_device_id")
    .msToken("your_ms_token")
    .verifyFp("your_verify_fp")
    .fp("your_fp")
    .webId("your_webid")
    .uifid("your_uifid")
    .cookie("your_full_cookie")
    .tsSign("your_ts_sign")
    .ticket("your_ticket")
    .certificatePem("your_certificate_pem")
    .publicKeyPem("your_public_key_pem")
    .privateKeyPem("your_private_key_pem")
    .build();

DouyinMessageSDK sdk = new DouyinMessageSDK(config);
```

推荐使用 `try-with-resources` 自动释放连接资源：

```java
try (DouyinMessageSDK sdk = new DouyinMessageSDK(config)) {
    // do something
}
```

### 2. 配置消息监听器与连接监听器

如果你需要通过 WebSocket 接收实时消息，可以先注册监听器，再调用 `connect()`：

```java
import com.dy_web_api.sdk.message.listener.ConnectionListener;
import com.dy_web_api.sdk.message.listener.MessageListener;
import com.dy_web_api.sdk.message.model.ConnectionStatus;
import com.dy_web_api.sdk.message.model.DouyinMessage;

sdk.setMessageListener(new MessageListener() {
    @Override
    public void onMessageReceived(DouyinMessage message) {
        System.out.println("收到消息内容: " + message.getContent());
        System.out.println("发送者ID: " + message.getSenderId());
        System.out.println("会话ID: " + message.getConversationId());
        System.out.println("消息类型: " + message.getMessageType());
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
});

sdk.setConnectionListener(new ConnectionListener() {
    @Override
    public void onStatusChanged(ConnectionStatus status) {
        System.out.println("连接状态: " + status);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
});

sdk.connect().get();
```

你也可以通过下面两个方法主动检查状态：

```java
System.out.println(sdk.getConnectionStatus());
System.out.println(sdk.isConnected());
```

### 3. 发送文本消息

文本消息发送不依赖 WebSocket 长连接，可以直接调用：

```java
String conversationId = "0:1:123456789:987654321";
Long conversationShortId = 7473797002657038875L;

boolean success = sdk.sendMessage(
    conversationId,
    conversationShortId,
    "你好，这是一条文本消息",
    false
).get();

System.out.println("发送结果: " + success);
```

参数说明：

- `conversationId`：会话 ID
- `conversationShortId`：会话短 ID
- `content`：消息文本
- `isGroup`：是否群聊，单聊传 `false`

### 4. 发送图片消息

推荐直接使用 SDK 已封装好的上传并发送接口：

```java
boolean success = sdk.uploadAndSendImageMessage(
    conversationId,
    conversationShortId,
    "/path/to/demo.jpg",
    false
).get();

System.out.println("图片发送结果: " + success);
```

如果你的图片已经在内存里，也可以使用字节数组版本：

```java
byte[] imageData = Files.readAllBytes(Path.of("/path/to/demo.jpg"));

boolean success = sdk.uploadAndSendImageMessage(
    conversationId,
    conversationShortId,
    imageData,
    "demo.jpg",
    false
).get();
```

如果你已经自行完成上传，也可以直接调用底层图片消息发送接口：

```java
sdk.sendImageMessage(
    conversationId,
    conversationShortId,
    "image_md5",
    "image_skey",
    "image_oid",
    102400,
    1080,
    720,
    false
).get();
```

### 5. 发送视频卡片消息

```java
sdk.sendVideoCardMessage(
    conversationId,
    conversationShortId,
    "your_aweme_item_id",
    false
).get();
```

### 6. 创建会话与给陌生人发消息

如果你已经拿到了对方用户 ID，可以直接建会话：

```java
Map<String, Object> conversation = sdk.createConversation(1234567890123456L);

String conversationId = (String) conversation.get("conversationId");
Long conversationShortId = (Long) conversation.get("conversationShortId");
```

如果你拿到的是 `secUid`，可以直接走完整流程：

```java
Map<String, Object> result = sdk.sendMsg2Stranger(
    "MS4wLjABAAAA...",
    "你好，很高兴认识你"
);

System.out.println(result);
```

### 7. 获取陌生人消息

```java
List<StrangerMessageInfo> list = sdk.fetchStrangerMessages(20);

for (StrangerMessageInfo item : list) {
    System.out.println("会话ID: " + item.getConversationId());
    System.out.println("发送方 secUid: " + item.getSenderSecUid());
    System.out.println("消息内容: " + item.getContentJson());
}
```

### 8. 获取用户信息

```java
import com.dy_web_api.sdk.message.model.UserInfo;

UserInfo userInfo = sdk.getUserInfo("MS4wLjABAAAA...");

System.out.println("昵称: " + userInfo.getNickname());
System.out.println("uid: " + userInfo.getUid());
System.out.println("签名: " + userInfo.getSignature());
System.out.println("粉丝数: " + userInfo.getFollowerCount());
System.out.println("作品数: " + userInfo.getAwemeCount());
```

### 9. 获取用户作品

获取默认数量：

```java
UserPostResponse response = sdk.getUserPosts("MS4wLjABAAAA...");
System.out.println(response.getAwemeList().size());
```

自定义分页：

```java
UserPostResponse response = sdk.getUserPosts("MS4wLjABAAAA...", "0", 18);

System.out.println("hasMore: " + response.getHasMore());
System.out.println("maxCursor: " + response.getMaxCursor());
```

自动分页拉满指定数量：

```java
UserPostResponse response = sdk.getUserPostsWithCount("MS4wLjABAAAA...", 100);
System.out.println("实际获取数量: " + response.getAwemeList().size());
```

按页读取：

```java
UserPostResponse page2 = sdk.getUserPostsByPage("MS4wLjABAAAA...", 2, 20);
```

### 10. 发送视频评论

纯文本评论：

```java
String commentId = sdk.commentVideo("your_aweme_id", "这是一个测试评论");
System.out.println("评论ID: " + commentId);
```

已知图片 URI 的评论：

```java
String commentId = sdk.commentVideoWithImage(
    "your_aweme_id",
    "这是带图评论",
    "your_image_uri",
    1080,
    720
);
```

本地图片上传后再评论：

```java
String commentId = sdk.uploadImageAndCommentVideo(
    "your_aweme_id",
    "这是一条图片评论",
    "/path/to/demo.jpg"
);
```

字节数组版本：

```java
byte[] imageData = Files.readAllBytes(Path.of("/path/to/demo.jpg"));

String commentId = sdk.uploadImageAndCommentVideo(
    "your_aweme_id",
    "这是一条图片评论",
    imageData,
    "demo.jpg"
);
```

### 11. 获取视频评论列表

默认读取第一页：

```java
VideoCommentListResponse response = sdk.getVideoCommentList("your_aweme_id");
System.out.println(response.getComments().size());
```

指定游标与数量：

```java
VideoCommentListResponse response = sdk.getVideoCommentList("your_aweme_id", 0L, 20);

System.out.println("总数: " + response.getTotal());
System.out.println("是否还有更多: " + response.isHasMore());
```

### 12. 主动断开连接

```java
sdk.disconnect().get();
```

## 配置说明

`DouyinConfig` 中的参数已经在代码内补充了按功能划分的注释，可直接查看：

- [DouyinConfig.java](src/main/java/com/dy_web_api/sdk/message/config/DouyinConfig.java)

按功能可以粗略分为以下几组：

| 功能 | 最小必需参数 |
| --- | --- |
| SDK 初始化 | `sessionId`、`userId` |
| WebSocket 消息接收 | `sessionId`、`userId`、`accessKey`、`deviceId` |
| 私信发送 | `sessionId`、`userId`，推荐补充 `msToken`、`verifyFp`、`fp` |
| 用户信息 / 用户作品 | `sessionId`，推荐补充 `msToken`、`verifyFp`、`webId`、`uifid` |
| 图片上传 | `sessionId`、`userId`，推荐补充 `msToken`、`webId`、`uifid`、`fp` |
| 视频评论 | `sessionId`、`cookie`、`msToken`、`verifyFp`、`fp`、`webId`、`uifid`、`tsSign`、`ticket`、`certificatePem`、`publicKeyPem`、`privateKeyPem` |

## 项目结构

```text
src/main/java/com/dy_web_api/sdk/
├── KolDyMsgApplication.java
└── message
    ├── client        # WebSocket 客户端
    ├── config        # 配置模型
    ├── exception     # 异常与错误码
    ├── handler       # 具体能力处理器
    ├── listener      # 回调监听器
    ├── model         # 业务数据模型
    ├── protobuf      # protobuf 结构
    ├── service       # 上传相关服务
    └── utils         # 工具类
```

## 构建与测试

```bash
mvn clean compile
mvn test
```

说明：

- 当前测试代码中仍包含较强的联调属性，依赖真实环境参数
- 如果作为二次开发基础项目，建议优先补充 mock 化测试与脱敏示例

## 开源说明

本项目用于工程研究、接口封装与学习交流。

在使用前请注意：

- 请勿提交任何真实 `sessionId`、`cookie`、证书、私钥或账号信息
- 请在你自己的环境中完成参数注入和密钥管理
- 请自行评估目标平台的使用条款、风控与合规要求


## 交流学习

![交流学习二维码](docs/images/contact-qrcode.png)


## 贡献

欢迎通过以下方式参与：

- 提交 Issue 反馈问题
- 提交 Pull Request 改进代码
- 完善文档、示例和测试


