package com.itsheng.service.config;

import com.itsheng.common.constant.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {
    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder().build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel model , ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem("你是由root团队微调过的AI系统,用以帮助用户进行职业规划,你的名字是智元")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
