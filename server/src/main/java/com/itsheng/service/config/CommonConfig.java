package com.itsheng.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.constant.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
@EnableAsync
public class CommonConfig implements AsyncConfigurer {

    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder().build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel model , ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem("你是由 root 团队微调过的 AI 系统，用以帮助用户进行职业规划，你的名字是职引")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 简历分析专用 ChatClient
     * 用于简历结构化解析，不使用对话历史
     */
    @Bean
    public ChatClient resumeAnalysisChatClient(OpenAiChatModel model) {
        return ChatClient
                .builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * ObjectMapper Bean，用于 JSON 解析
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    //配置 pgVectorStore

    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, OpenAiEmbeddingModel embeddingModel){
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(false)
                .schemaName("ai_career_plan")
                .vectorTableName("user_vector_store")
                .build();
    }

    /**
     * 异步执行器配置
     */
    @Override
    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("resume-analysis-");
        executor.initialize();
        return executor;
    }
}
