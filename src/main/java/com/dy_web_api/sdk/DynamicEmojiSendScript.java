package com.dy_web_api.sdk;

import com.dy_web_api.sdk.message.DouyinMessageSDK;
import com.dy_web_api.sdk.message.config.DouyinConfig;

import java.util.concurrent.TimeUnit;

/**
 * 动态表情发送测试脚本。
 *
 * <p>运行前请先设置环境变量：
 * <ul>
 *   <li>DY_SESSION_ID</li>
 *   <li>DY_USER_ID</li>
 *   <li>DY_EMOJI_NAME（可选，默认：续火花）</li>
 * </ul>
 */
public class DynamicEmojiSendScript {

    private static final String CONVERSATION_ID = "";  // 替换为实际的 conversationId
    private static final long CONVERSATION_SHORT_ID = 0L; // 替换为实际的 conversationShortId
    private static final String DEFAULT_EMOJI_NAME = "笑死";

    public static void main(String[] args) throws Exception {
        String sessionId = System.getenv("DY_SESSION_ID");
        String userId = System.getenv("DY_USER_ID");
        String emojiName = resolveEmojiName(args);

        if (isBlank(sessionId) || isBlank(userId)) {
            System.err.println("请先设置环境变量 DY_SESSION_ID 和 DY_USER_ID");
            System.exit(1);
        }

        DouyinConfig config = DouyinConfig.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        try (DouyinMessageSDK sdk = new DouyinMessageSDK(config)) {
            boolean success = sdk.sendDynamicEmojiMessage(
                    CONVERSATION_ID,
                    CONVERSATION_SHORT_ID,
                    emojiName,
                    false
            ).get(30, TimeUnit.SECONDS);

            System.out.println("conversationId: " + CONVERSATION_ID);
            System.out.println("conversationShortId: " + CONVERSATION_SHORT_ID);
            System.out.println("emojiName: " + emojiName);
            System.out.println("sendDynamicEmojiMessage success: " + success);
        }
    }

    private static String resolveEmojiName(String[] args) {
        if (args != null && args.length > 0 && !isBlank(args[0])) {
            return args[0].trim();
        }
        String envEmojiName = System.getenv("DY_EMOJI_NAME");
        if (!isBlank(envEmojiName)) {
            return envEmojiName.trim();
        }
        return DEFAULT_EMOJI_NAME;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
