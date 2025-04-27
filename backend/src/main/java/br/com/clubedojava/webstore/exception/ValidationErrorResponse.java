package br.com.clubedojava.webstore.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ValidationErrorResponse {
    public ValidationErrorResponse(LocalDateTime now, int value, String reasonPhrase, String validationError, Map<String, String> errors) {
    }
}
