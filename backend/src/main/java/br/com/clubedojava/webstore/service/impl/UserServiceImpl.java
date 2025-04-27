package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.RegisterRequestDTO;
import br.com.clubedojava.webstore.dto.UserDTO;
import br.com.clubedojava.webstore.dto.UserDetailsImpl;
import br.com.clubedojava.webstore.model.User;
import br.com.clubedojava.webstore.repository.UserRepository;
import br.com.clubedojava.webstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return UserDetailsImpl.build(user);
    }

    @Override
    @Suspendable
    public CompletableFuture<UserDTO> registerUser(RegisterRequestDTO registerRequest) {
        // Cria um novo usuário com os dados do request
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);

        return CompletableFuture.completedFuture(convertToDTO(savedUser));
    }

    @Override
    @Suspendable
    public CompletableFuture<Optional<UserDTO>> findById(Long id) {
        return CompletableFuture.completedFuture(
                userRepository.findById(id)
                        .map(this::convertToDTO)
        );
    }

    @Override
    @Suspendable
    public CompletableFuture<Optional<UserDTO>> findByEmail(String email) {
        return CompletableFuture.completedFuture(
                userRepository.findByEmail(email)
                        .map(this::convertToDTO)
        );
    }

    @Override
    @Suspendable
    public CompletableFuture<UserDTO> updateUser(Long id, UserDTO userDTO) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

            // Atualiza os dados básicos do usuário
            user.setName(userDTO.getName());

            User updatedUser = userRepository.save(user);

            return convertToDTO(updatedUser);
        });
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }
}