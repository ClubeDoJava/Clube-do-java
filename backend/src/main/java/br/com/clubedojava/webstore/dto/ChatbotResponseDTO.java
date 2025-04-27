package br.com.clubedojava.webstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDTO {
    private String answer;
    private String intent;
    private Float confidence;
}