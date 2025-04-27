package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.RegisterRequestDTO;
import br.com.clubedojava.webstore.dto.UserDTO;
import br.com.clubedojava.webstore.dto.UserDetailsImpl;
import br.com.clubedojava.webstore.service.impl.Suspendable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UserService extends UserDetailsService {

    @Suspendable
    CompletableFuture<UserDTO> registerUser(RegisterRequestDTO registerRequest);

    @Suspendable
    CompletableFuture<Optional<UserDTO>> findById(Long id);

    @Suspendable
    CompletableFuture<Optional<UserDTO>> findByEmail(String email);

    @Suspendable
    CompletableFuture<UserDTO> updateUser(Long id, UserDTO userDTO);
}