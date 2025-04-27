package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.RegisterRequestDTO;
import br.com.clubedojava.webstore.dto.UserDTO;
import br.com.clubedojava.webstore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<UserDTO>> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        return userService.registerUser(registerRequest)
                .thenApply(user -> new ResponseEntity<>(user, HttpStatus.CREATED));
    }

    // O login é tratado pelo JwtAuthenticationFilter
}
