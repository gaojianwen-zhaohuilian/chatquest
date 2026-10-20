package com.chatquest;

import com.chatquest.platform.AdapterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用入口。
 */
@SpringBootApplication
public class ChatQuestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatQuestApplication.class, args);
    }

    /**
     * 注册 AdapterRegistry，Spring 自动把所有 PlatformAdapter 实现注入进来。
     *
     * <p>新增平台时只需在对应类上加 {@code @Component}，这里无需任何改动。
     */
    @Configuration
    static class AdapterConfiguration {

        @Bean
        AdapterRegistry adapterRegistry(java.util.List<com.chatquest.platform.PlatformAdapter> adapters) {
            return new AdapterRegistry(adapters);
        }
    }
}
