package com.chatquest.platform.model;

import com.chatquest.platform.PlatformType;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 统一消息模型 —— 各平台 Adapter 接收到的原始消息都会归一化成这个结构。
 *
 * <p>这是整个抽象层的核心价值：<b>下游业务（幂等拦截、LLM 抽取、任务入库）完全不感知平台差异</b>。
 *
 * <p>{@link #rawPayload} 保留平台原始 JSON，用于调试、排查协议问题，以及未来可能的
 * 平台特性回溯。剥离它会让问题排查极其困难。
 *
 * @param platform      来源平台
 * @param messageId     平台侧唯一消息 ID，幂等键的组成部分
 * @param conversationId 会话 ID：群聊为群 ID，私聊为双方组合
 * @param userId        发送者平台侧 ID
 * @param userName      发送者显示名
 * @param content       纯文本内容，已剥离平台前缀（如 QQ 的 @机器人名）
 * @param receivedAt    <b>消息在平台侧被发送的时间</b>，不是本地接收时间
 * @param rawPayload    平台原始报文，永不为空
 */
public record UnifiedMessage(
        PlatformType platform,
        String messageId,
        String conversationId,
        String userId,
        String userName,
        String content,
        LocalDateTime sentAt,
        String rawPayload
) {

    /**
     * 幂等键：platform + messageId。
     *
     * <p>用平台做前缀是必须的 —— 不同平台的 messageId 可能撞号。
     *
     * @return 形如 {@code DINGTALK:msg_abc123}
     */
    public String idempotentKey() {
        return platform.name() + ":" + messageId;
    }

    /**
     * 当前时间兜底。用于平台未上报时间戳的极端情况。
     */
    public LocalDateTime sentAtOrNow() {
        return sentAt != null ? sentAt : LocalDateTime.now();
    }
}
