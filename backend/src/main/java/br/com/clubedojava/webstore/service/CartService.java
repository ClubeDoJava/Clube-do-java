package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.CartDTO;
import br.com.clubedojava.webstore.dto.CartItemDTO;
import br.com.clubedojava.webstore.model.Cart;
import br.com.clubedojava.webstore.service.impl.Suspendable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CartService {

    @Suspendable
    CompletableFuture<CartDTO> getCart(String username);

    Optional<Cart> getCartByUser(Long userId);

    @Suspendable
    CompletableFuture<CartDTO> addItem(String username, CartItemDTO cartItemDTO);

    @Suspendable
    CompletableFuture<CartDTO> updateItemQuantity(String username, Long itemId, int quantity);

    @Suspendable
    CompletableFuture<CartDTO> removeItem(String username, Long itemId);

    @Suspendable
    CompletableFuture<Void> clearCart(Long cartId);
}