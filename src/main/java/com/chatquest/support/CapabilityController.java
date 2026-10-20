package com.chatquest.support;

import com.chatquest.platform.AdapterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 平台能力自检接口。
 *
 * <p>启动后先打这个接口，确认 Adapter 注册和能力声明是否符合预期，
 * 再去调试具体平台。这比直接连平台再排查日志快得多。
 *
 * <p>D1 完成后访问：{@code http://localhost:8080/api/capabilities}
 */
@RestController
@RequestMapping("/api")
public class CapabilityController {

    private final AdapterRegistry registry;

    public CapabilityController(AdapterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "registeredPlatforms", registry.registeredPlatforms()
        );
    }

    /**
     * 查看每个已注册平台的能力声明。
     *
     * <p>重点看 {@code noProactivePush} 字段 —— 非空说明这些平台的临期提醒
     * 必须走降级通道，这就是能力不对称的具体表现。
     */
    @GetMapping("/capabilities")
    public Map<String, Object> capabilities() {
        var detail = registry.all().stream()
                .map(a -> {
                    var c = a.capabilities();
                    return Map.of(
                            "platform", a.platform().name(),
                            "groupRead", c.groupRead(),
                            "directRead", c.directRead(),
                            "proactivePush", c.proactivePush(),
                            "interactiveCard", c.interactiveCard(),
                            "atMentionTrigger", c.atMentionTrigger(),
                            "replyRateLimit", c.replyRateLimit()
                    );
                })
                .toList();

        List<String> noPush = registry.platformsWithoutProactivePush().stream()
                .map(Enum::name)
                .toList();

        return Map.of(
                "platforms", detail,
                "noProactivePush", noPush,
                "reminderFallbackRequired", !noPush.isEmpty(),
                "note", noPush.isEmpty()
                        ? "所有已注册平台均可主动推送提醒"
                        : "以下平台无法主动推送，临期提醒需降级：" + noPush
        );
    }
}
