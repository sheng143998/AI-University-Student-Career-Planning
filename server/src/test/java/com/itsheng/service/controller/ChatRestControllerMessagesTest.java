package com.itsheng.service.controller;

import com.itsheng.pojo.dto.ChatSendMessageDTO;
import com.itsheng.service.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatRestControllerMessagesTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class, ChatRestController.class)
            .withBean(WebMvcAutoConfiguration.class)
            .withBean(DispatcherServletAutoConfiguration.class)
            .withBean(HttpMessageConvertersAutoConfiguration.class);

    @Test
    void formalMessageEndpointStillStreamsWhenDebugEndpointDisabled() {
        contextRunner.run(context -> {
            ChatService chatService = context.getBean(ChatService.class);
            when(chatService.sendMessage(any(ChatSendMessageDTO.class))).thenReturn(Flux.just("formal answer"));
            var mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext()).build();

            MvcResult result = mockMvc.perform(post("/api/chat/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"conversationId\":42,\"content\":\"career advice\",\"resumeId\":123}"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=utf-8"))
                    .andReturn();

            assertEquals("formal answer", dispatched.getResponse().getContentAsString());
            verify(chatService).sendMessage(any(ChatSendMessageDTO.class));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
        @Bean
        ChatService chatService() {
            return mock(ChatService.class);
        }
    }
}
