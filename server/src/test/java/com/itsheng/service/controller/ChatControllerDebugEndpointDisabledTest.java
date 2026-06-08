package com.itsheng.service.controller;

import com.itsheng.service.client.PythonChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerDebugEndpointDisabledTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class, ChatController.class)
            .withBean(WebMvcAutoConfiguration.class)
            .withBean(DispatcherServletAutoConfiguration.class)
            .withBean(HttpMessageConvertersAutoConfiguration.class);

    @Test
    void debugEndpointIsNotRegisteredByDefault() {
        contextRunner.run(context -> {
            PythonChatClient pythonChatClient = context.getBean(PythonChatClient.class);
            MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
                    .build()
                    .perform(get("/ai/chat").param("prompt", "career advice"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(pythonChatClient);
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
