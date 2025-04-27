package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.ChatbotResponseDTO;
import br.com.clubedojava.webstore.service.impl.Suspendable;

import java.util.concurrent.CompletableFuture;

public interface ChatbotService {

    @Suspendable
    CompletableFuture<ChatbotResponseDTO> processMessage(String message);

    @Suspendable
    CompletableFuture<String[]> getAvailableIntents();
}