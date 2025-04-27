package br.com.clubedojava.webstore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ShippingException extends RuntimeException {

    public ShippingException(String message) {
        super(message);
    }

    public ShippingException(String message, Throwable cause) {
        super(message, cause);
    }
}