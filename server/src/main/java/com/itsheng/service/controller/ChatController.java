package com.itsheng.service.controller;

import com.itsheng.service.client.PythonChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(prefix = "fuchuang.ai.python", name = "debug-chat-endpoint-enabled", havingValue = "true", matchIfMissing = false)
public class ChatController {
    private final PythonChatClient pythonChatClient;

    @GetMapping(value = "/ai/chat", produces = "text/html;charset=utf-8")
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

    @RequestMapping(value = "/ai/chat", method = {
            RequestMethod.HEAD,
            RequestMethod.OPTIONS,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE
    })
    public ResponseEntity<Void> rejectNonGetChat() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
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
