package com.itsheng.service.controller;

import com.itsheng.service.client.PythonChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerDebugEndpointEnabledTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withPropertyValues("fuchuang.ai.python.debug-chat-endpoint-enabled=true")
            .withUserConfiguration(TestConfiguration.class, ChatController.class)
            .withBean(WebMvcAutoConfiguration.class)
            .withBean(DispatcherServletAutoConfiguration.class)
            .withBean(HttpMessageConvertersAutoConfiguration.class);

    @Test
    void debugEndpointCallsPythonClientWhenExplicitlyEnabled() {
        contextRunner.run(context -> {
            PythonChatClient pythonChatClient = context.getBean(PythonChatClient.class);
            when(pythonChatClient.complete(0L, 42L, "career advice", null, Collections.emptyList(), null))
                    .thenReturn(PythonChatClient.ChatCompletionResult.builder()
                            .content("stub answer")
                            .build());

            var mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .build();
            MvcResult asyncResult = mockMvc
                    .perform(get("/ai/chat").param("prompt", "career advice").param("chatId", "42"))
                    .andExpect(request().asyncStarted())
                    .andReturn();
            MvcResult result = mockMvc
                    .perform(asyncDispatch(asyncResult))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=utf-8"))
                    .andReturn();

            assertEquals("stub answer", result.getResponse().getContentAsString());
            verify(pythonChatClient).complete(0L, 42L, "career advice", null, Collections.emptyList(), null);
            verifyNoMoreInteractions(pythonChatClient);
        });
    }

    @Test
    void debugEndpointCanBeEnabledByOsEnvironmentVariableName() {
        new WebApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource(
                                "test-os-env",
                                Map.<String, Object>of("FUCHUANG_AI_PYTHON_DEBUG_CHAT_ENDPOINT_ENABLED", "true")
                        )
                ))
                .withUserConfiguration(TestConfiguration.class, ChatController.class)
                .withBean(WebMvcAutoConfiguration.class)
                .withBean(DispatcherServletAutoConfiguration.class)
                .withBean(HttpMessageConvertersAutoConfiguration.class)
                .run(context -> {
                    PythonChatClient pythonChatClient = context.getBean(PythonChatClient.class);
                    when(pythonChatClient.complete(0L, 7L, "env advice", null, Collections.emptyList(), null))
                            .thenReturn(PythonChatClient.ChatCompletionResult.builder()
                                    .content("e")
                                    .build());

                    var mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                            .build();
                    MvcResult asyncResult = mockMvc
                            .perform(get("/ai/chat").param("prompt", "env advice").param("chatId", "7"))
                            .andExpect(request().asyncStarted())
                            .andReturn();
                    MvcResult result = mockMvc
                            .perform(asyncDispatch(asyncResult))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType("text/html;charset=utf-8"))
                            .andReturn();

                    assertEquals("e", result.getResponse().getContentAsString());
                    verify(pythonChatClient).complete(0L, 7L, "env advice", null, Collections.emptyList(), null);
                    verifyNoMoreInteractions(pythonChatClient);
                });
    }

    @Test
    void debugEndpointRejectsNonGetMethodsWhenEnabled() {
        contextRunner.run(context -> {
            PythonChatClient pythonChatClient = context.getBean(PythonChatClient.class);
            var mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext()).build();

            mockMvc.perform(post("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(put("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(delete("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(patch("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(options("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(head("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoMoreInteractions(pythonChatClient);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
        @Bean
        PythonChatClient pythonChatClient() {
            return mock(PythonChatClient.class);
        }
    }
}
