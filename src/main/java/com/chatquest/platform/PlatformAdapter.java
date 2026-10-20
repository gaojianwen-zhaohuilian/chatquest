package com.chatquest.platform;

import com.chatquest.platform.model.PlatformCapabilities;
import com.chatquest.platform.model.UnifiedMessage;

/**
 * 平台适配器 —— 所有 IM 平台的统一接入契约。
 *
 * <p><b>这是整个项目最重要的抽象。</b>
 *
 * <p>四个平台的接入方式完全不同：
 * <ul>
 *   <li>钉钉：Stream 模式，WSS 长连接，主动推送</li>
 *   <li>QQ：botgo，WebSocket，被 @ 触发，被动回复有频控</li>
 *   <li>企业微信：WebSocket 网关</li>
 *   <li>微信 iLink：HTTP 轮询，有游标</li>
 * </ul>
 * 但业务层只依赖这一个接口。
 *
 * <p><b>实现者的铁律：所有平台特有的处理必须在这一层完成，绝不能泄漏到下游。</b>
 * 例如 QQ 消息里的 {@code @机器人名 } 前缀、微信的 CRLF、钉钉的 AES 加密，
 * 都要在各自 Adapter 内消化掉。泄漏一个，业务层就会长出平台判断分支，抽象即失效。
 *
 * <p>新增平台的成本：一个实现类约 150 行，核心逻辑零改动。
 */
public interface PlatformAdapter {

    /**
     * 本适配器对应的平台。
     */
    PlatformType platform();

    /**
     * 声明本平台的能力。
     *
     * <p>业务层据此决定推送策略，禁止用 {@code if (platform == ...)} 代替本方法。
     *
     * @return 能力声明，永不为 null
     */
    PlatformCapabilities capabilities();

    /**
     * 建立连接并开始接收消息。
     *
     * <p>实现需自行处理心跳保活、断线重连与退避。连接建立后应持续运行，
     * 收到消息后调用传入的 consumer。
     *
     * @param consumer 消息消费者，由核心层提供，负责幂等、抽取、入库
     * @throws PlatformException 连接失败或鉴权失败
     */
    void connect(MessageConsumer consumer) throws PlatformException;

    /**
     * 主动断开连接，释放资源。用于应用关闭时优雅停机。
     */
    void disconnect();

    /**
     * 回复一条消息（被动回复，受频控约束）。
     *
     * @param message 要回复的目标消息
     * @param text    回复内容，支持平台支持的格式（如 Markdown）
     */
    void reply(UnifiedMessage message, String text) throws PlatformException;

    /**
     * 主动推送提醒（不受频控约束，但受平台能力限制）。
     *
     * <p><b>调用前必须检查 {@link #capabilities()#canPushReminder()}。</b>
     * 微信 iLink 会抛出 {@link UnsupportedOperationException}，
     * 业务层应提前判断并降级，不要依赖异常来控制流程。
     *
     * @param userId 目标用户
     * @param text   提醒内容
     * @throws UnsupportedOperationException 平台不支持主动推送
     */
    void pushReminder(String userId, String text) throws PlatformException;

    /**
     * 消息消费者接口。核心层实现，Adapter 只管投递。
     */
    @FunctionalInterface
    interface MessageConsumer {
        void accept(UnifiedMessage message);
    }

    /**
     * 平台接入异常。
     *
     * <p>分三类，因为处理策略完全不同：
     * <ul>
     *   <li>AUTH —— 凭证失效，需要人工介入</li>
     *   <li>NETWORK —— 可重试，Adapter 内部应自行重试</li>
     *   <li>PROTOCOL —— 报文解析失败，通常是平台改版了</li>
     * </ul>
     */
    class PlatformException extends RuntimeException {

        private final Type type;

        public PlatformException(Type type, String message) {
            super(message);
            this.type = type;
        }

        public PlatformException(Type type, String message, Throwable cause) {
            super(message, cause);
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public enum Type {
            AUTH, NETWORK, PROTOCOL
        }
    }
}
