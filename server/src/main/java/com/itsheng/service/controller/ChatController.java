package com.itsheng.service.controller;

import com.itsheng.service.client.PythonChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final PythonChatClient pythonChatClient;

    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId){
        Long conversationId = parseConversationId(chatId);
        String userPrompt = prompt == null ? "" : prompt;
        PythonChatClient.ChatCompletionResult completion = pythonChatClient.complete(
                0L,
                conversationId,
                userPrompt,
                null,
                java.util.Collections.emptyList(),
                null
        );
        String content = completion.getContent() == null ? "" : completion.getContent();
        return Flux.fromArray(content.split(""));
    }

    private Long parseConversationId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(chatId);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
