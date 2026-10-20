package com.chatquest.platform;

import com.chatquest.platform.model.PlatformCapabilities;
import com.chatquest.platform.model.UnifiedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 钉钉适配器 —— 骨架占位，第 1 周 D3 实现。
 *
 * <p><b>接入要点（D3 实操时注意）</b>：
 * <ol>
 *   <li>钉钉开发者后台创建「企业内部应用」→ 添加「机器人」能力</li>
 *   <li>接收模式必须选 <b>Stream 模式</b>（WSS 出站长连接），
 *       这样无需公网回调地址，学生机上可直接运行</li>
 *   <li>权限只申请三个，勿贪多（贪多会触发管理员额外审核）：
 *       <ul>
 *         <li>{@code im:message.group_at_msg:readonly} 读群 @ 消息</li>
 *         <li>{@code im:message.p2p_msg:readonly} 读私聊消息</li>
 *         <li>{@code contact:user.employee_id:readonly} 读用户身份</li>
 *       </ul>
 *       <b>不要申请 {@code im:message.group_msg}</b>（读所在群全部消息），
 *       那是敏感权限，审核严且不符合本项目「用户主动喂」的设计原则。</li>
 *   <li>凭证读取 Client ID / Client Secret / Agent ID，
 *       <b>一律从环境变量取，绝不硬编码</b>（application.yml 已配好占位）</li>
 *   <li>钉钉回调/推送数据默认 AES-256-CBC 加密，必须先解密再处理。
 *       这与飞书的「仅验签」不同，是钉钉特有的坑。</li>
 * </ol>
 *
 * <p><b>待填坑位（D3 记录到 README 第五节）</b>：
 * Stream 模式的 App 名称混淆会导致连接失败，日志里不明显。
 */
@Component
public class DingTalkAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(DingTalkAdapter.class);

    @Override
    public PlatformType platform() {
        return PlatformType.DINGTALK;
    }

    @Override
    public PlatformCapabilities capabilities() {
        return PlatformCapabilities.dingTalk();
    }

    @Override
    public void connect(MessageConsumer consumer) throws PlatformException {
        // D3 实现：建立 WSS 长连接，启动心跳，收到消息后解密 → 归一化 → consumer.accept()
        throw new PlatformException(
                PlatformException.Type.NETWORK,
                "DingTalkAdapter 尚未实现，请完成第 1 周 D3"
        );
    }

    @Override
    public void disconnect() {
        // D3 实现：关闭 WSS 连接与定时心跳任务
    }

    @Override
    public void reply(UnifiedMessage message, String text) {
        // D3 实现：调用钉钉机器人回复接口，注意被动回复有时效窗口
    }

    @Override
    public void pushReminder(String userId, String text) {
        // D3 实现：调用主动消息接口
        // 钉钉支持主动推送，无需做能力降级
    }
}
