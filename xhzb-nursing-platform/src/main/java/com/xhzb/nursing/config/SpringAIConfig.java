package com.xhzb.nursing.config;

import com.xhzb.nursing.service.impl.RedisChatMemoryService;
import com.xhzb.nursing.tool.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import com.xhzb.nursing.constants.SystemConstants;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    /**
     * 声明chatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, RedisChatMemoryService redisChatMemoryService, WeatherTools weatherTools) {
        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem(SystemConstants.prompt)
                //.defaultTools(weatherTools)
                .defaultAdvisors(new SimpleLoggerAdvisor(),//添加日志处理器（aop切面编程机制
                        MessageChatMemoryAdvisor.builder(redisChatMemoryService).build())//将我们写的RedisChatMemoryService包装成切面处理器
                .build();

    }
}
