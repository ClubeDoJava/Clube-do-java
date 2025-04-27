package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.ChatbotMessageDTO;
import br.com.clubedojava.webstore.dto.ChatbotResponseDTO;
import br.com.clubedojava.webstore.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<ChatbotResponseDTO>> processMessage(
            @RequestBody ChatbotMessageDTO messageDTO) {
        return chatbotService.processMessage(messageDTO.getMessage())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/intents")
    public CompletableFuture<ResponseEntity<String[]>> getAvailableIntents() {
        return chatbotService.getAvailableIntents()
                .thenApply(ResponseEntity::ok);
    }
}


