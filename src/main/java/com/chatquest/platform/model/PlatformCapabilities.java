package com.chatquest.platform.model;

/**
 * 平台能力声明 —— 本项目最核心的设计。
 *
 * <p><b>解决的问题</b>：四个平台的推送能力并不对等。若写成
 * {@code if (platform == WECHAT_ILINK) { /* 不能主动推 */ }} 这类分支，
 * 业务代码会迅速腐化成平台判断的集合，每加一个平台就要改一轮核心逻辑。
 *
 * <p><b>解决方式</b>：每个 Adapter 自行声明它具备什么能力，业务层只根据能力做决策。
 * 这样做的直接收益是<b>新增平台只需实现 Adapter，核心逻辑零改动</b>。
 *
 * @param groupRead        能否读取群聊消息（微信 iLink 为 false）
 * @param directRead       能否读取私聊消息（四平台均支持）
 * @param proactivePush    能否主动推送消息（微信 iLink 仅 24h 内有效）
 * @param interactiveCard  是否支持交互卡片与按钮回调（微信 iLink 为 false）
 * @param atMentionTrigger 是否需要 @ 机器人才能触发（钉钉、QQ 为 true）
 * @param replyRateLimit   被动回复频控（条/分钟），0 表示无限制
 */
public record PlatformCapabilities(
        boolean groupRead,
        boolean directRead,
        boolean proactivePush,
        boolean interactiveCard,
        boolean atMentionTrigger,
        int replyRateLimit
) {

    /**
     * 钉钉：能力最完整，唯一限制是被 @ 才触发。
     */
    public static PlatformCapabilities dingTalk() {
        return new PlatformCapabilities(true, true, true, true, true, 0);
    }

    /**
     * QQ：需申请群消息权限，被动回复有频控。
     */
    public static PlatformCapabilities qq() {
        return new PlatformCapabilities(true, true, true, false, true, 5);
    }

    /**
     * 企业微信：能力完整。
     */
    public static PlatformCapabilities weCom() {
        return new PlatformCapabilities(true, true, true, true, true, 0);
    }

    /**
     * 微信 iLink：能力短板通道。
     *
     * <p>不支持群聊、不能主动发起、无交互卡片。这三个 false 就是
     * 业务层必须实现降级策略的原因。
     */
    public static PlatformCapabilities wechatIlink() {
        return new PlatformCapabilities(false, true, false, false, false, 0);
    }

    /**
     * 是否可以回推提醒。
     *
     * <p>注意微信 iLink 的 {@code proactivePush=false} 意味着
     * <b>临期提醒推不回微信</b>，业务层必须降级到 Web 端或其他通道。
     */
    public boolean canPushReminder() {
        return proactivePush;
    }

    /**
     * 是否可以发交互卡片让用户点击确认。
     */
    public boolean canSendInteractiveCard() {
        return interactiveCard;
    }
}
