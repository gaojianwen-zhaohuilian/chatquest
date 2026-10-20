package com.chatquest.platform;

import com.chatquest.platform.model.PlatformCapabilities;
import com.chatquest.platform.model.UnifiedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 适配器注册表 —— 核心层唯一接触平台的入口。
 *
 * <p>业务层通过它获取 Adapter 并消费消息，<b>不直接依赖任何具体平台实现</b>。
 *
 * <p>Spring 会自动把所有实现了 {@link PlatformAdapter} 的 Bean 注入进来，
 * 所以新增平台时只需把它注册成 Bean（{@code @Component}），这里无需任何改动。
 */
public class AdapterRegistry {

    private static final Logger log = LoggerFactory.getLogger(AdapterRegistry.class);

    private final List<PlatformAdapter> adapters = new ArrayList<>();

    /**
     * Spring 注入所有 Adapter 实现。
     *
     * @param adapterBeans 容器中所有 PlatformAdapter 实现
     */
    public AdapterRegistry(List<PlatformAdapter> adapterBeans) {
        // 校验平台唯一性：同一个平台出现两个实现是配置错误，必须尽早暴露
        for (PlatformAdapter adapter : adapterBeans) {
            boolean duplicated = adapters.stream()
                    .anyMatch(existing -> existing.platform() == adapter.platform());
            if (duplicated) {
                throw new IllegalStateException("平台 " + adapter.platform() + " 存在重复的 Adapter 实现");
            }
            adapters.add(adapter);
        }
        log.info("已注册 {} 个平台适配器: {}", adapters.size(), registeredPlatforms());
    }

    public List<PlatformAdapter> all() {
        return Collections.unmodifiableList(adapters);
    }

    public List<PlatformType> registeredPlatforms() {
        return adapters.stream().map(PlatformAdapter::platform).toList();
    }

    /**
     * 按平台取 Adapter。
     *
     * @return 未注册时返回 null，由调用方决定降级策略
     */
    public PlatformAdapter get(PlatformType platform) {
        return adapters.stream()
                .filter(a -> a.platform() == platform)
                .findFirst()
                .orElse(null);
    }

    /**
     * 启动所有已启用的 Adapter。
     *
     * <p><b>单个平台连接失败不应阻断其他平台启动</b> —— 这是容错设计，
     * QQ 挂了不影响钉钉正常工作。
     *
     * @param consumer 消息消费者
     */
    public void startAll(PlatformAdapter.MessageConsumer consumer) {
        for (PlatformAdapter adapter : adapters) {
            try {
                adapter.connect(consumer);
                PlatformCapabilities caps = adapter.capabilities();
                log.info("{} 接入成功 | 群读={} 私聊读={} 主动推送={} 交互卡片={} 频控={}条/min",
                        adapter.platform().getDisplayName(),
                        caps.groupRead(), caps.directRead(),
                        caps.proactivePush(), caps.interactiveCard(),
                        caps.replyRateLimit());
            } catch (PlatformAdapter.PlatformException e) {
                // 记录但不抛出：保证其他平台能正常启动
                log.error("{} 接入失败({})，该通道不可用: {}",
                        adapter.platform().getDisplayName(), e.getType(), e.getMessage());
            }
        }
    }

    /**
     * 优雅停机，关闭所有连接。
     */
    public void stopAll() {
        adapters.forEach(adapter -> {
            try {
                adapter.disconnect();
            } catch (RuntimeException e) {
                log.warn("{} 关闭连接时异常: {}", adapter.platform().getDisplayName(), e.getMessage());
            }
        });
    }

    /**
     * 找出所有不支持主动推送的平台。
     *
     * <p>提醒服务需要据此决定降级到哪个出口 —— 这是能力不对称最直接的体现。
     *
     * @return 能力短板平台列表，非空时提醒必须走降级通道
     */
    public List<PlatformType> platformsWithoutProactivePush() {
        return adapters.stream()
                .filter(a -> !a.capabilities().canPushReminder())
                .map(PlatformAdapter::platform)
                .toList();
    }
}
