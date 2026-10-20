package com.chatquest.platform;

/**
 * 平台类型枚举。
 *
 * <p>业务层用这个枚举做能力降级决策，但<strong>不要用 if (platform == XXX) 直接分支</strong>。
 * 正确做法是调用 {@link PlatformAdapter#capabilities()} 判断平台具备什么能力，
 * 从而实现「加新平台零改动」。
 */
public enum PlatformType {

    /** 钉钉：Stream 模式（WSS 长连接），能力最完整 */
    DINGTALK("钉钉"),

    /** QQ：官方开放平台 botgo（WebSocket），需个人实名认证 */
    QQ("QQ"),

    /** 企业微信：WebSocket 机器人网关 */
    WECOM("企业微信"),

    /**
     * 微信 iLink：官方开放的个人微信 Bot 协议（HTTP 轮询）。
     *
     * <p><b>能力短板通道</b>：不支持群聊、不能主动发起（context_token 24h 过期）、无交互卡片。
     * 这些限制正是本项目引入 {@link PlatformCapabilities} 能力探测的根因。
     *
     * <p>注意：这是 iLink 通道标识，<b>不等于</b>能读取用户全部聊天记录。
     * 该通道只能收到用户主动发给 Bot 的私聊消息。
     */
    WECHAT_ILINK("微信iLink");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
