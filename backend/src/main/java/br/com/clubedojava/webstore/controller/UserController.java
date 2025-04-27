package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.AddressDTO;
import br.com.clubedojava.webstore.dto.UserDTO;
import br.com.clubedojava.webstore.service.AddressService;
import br.com.clubedojava.webstore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @GetMapping("/me")
    public CompletableFuture<ResponseEntity<UserDTO>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .thenApply(userOpt -> userOpt
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verifica se o usuário está atualizando seu próprio perfil
        if (!userDetails.getUsername().equals(userDTO.getEmail()) &&
                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }

        return userService.updateUser(id, userDTO)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}/addresses")
    public CompletableFuture<ResponseEntity<Set<AddressDTO>>> getUserAddresses(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return addressService.findByUserId(id, userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{id}/addresses")
    public CompletableFuture<ResponseEntity<AddressDTO>> addUserAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO addressDTO,
            @AuthenticationPrincipal UserDetails userDetails) {

        return addressService.addAddress(id, addressDTO, userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public CompletableFuture<ResponseEntity<AddressDTO>> updateUserAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO addressDTO,
            @AuthenticationPrincipal UserDetails userDetails) {

        return addressService.updateAddress(userId, addressId, addressDTO, userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public CompletableFuture<ResponseEntity<Void>> deleteUserAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return addressService.deleteAddress(userId, addressId, userDetails.getUsername())
                .thenApply(v -> ResponseEntity.noContent().build());
    }
}