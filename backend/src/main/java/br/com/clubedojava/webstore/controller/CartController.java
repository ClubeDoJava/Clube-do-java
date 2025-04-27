package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.CartDTO;
import br.com.clubedojava.webstore.dto.CartItemDTO;
import br.com.clubedojava.webstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CompletableFuture<ResponseEntity<CartDTO>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        return cartService.getCart(userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/items")
    public CompletableFuture<ResponseEntity<CartDTO>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemDTO cartItemDTO) {
        return cartService.addItem(userDetails.getUsername(), cartItemDTO)
                .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/items/{itemId}")
    public CompletableFuture<ResponseEntity<CartDTO>> updateItemQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        return cartService.updateItemQuantity(userDetails.getUsername(), itemId, quantity)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/items/{itemId}")
    public CompletableFuture<ResponseEntity<CartDTO>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        return cartService.removeItem(userDetails.getUsername(), itemId)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping
    public CompletableFuture<ResponseEntity<Void>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        return cartService.getCart(userDetails.getUsername())
                .thenCompose(cart -> cartService.clearCart(cart.getId()))
                .thenApply(v -> ResponseEntity.noContent().build());
    }
}