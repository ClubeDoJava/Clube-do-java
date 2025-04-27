package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.CartDTO;
import br.com.clubedojava.webstore.dto.CartItemDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.model.Cart;
import br.com.clubedojava.webstore.model.CartItem;
import br.com.clubedojava.webstore.model.Product;
import br.com.clubedojava.webstore.model.User;
import br.com.clubedojava.webstore.repository.CartItemRepository;
import br.com.clubedojava.webstore.repository.CartRepository;
import br.com.clubedojava.webstore.repository.ProductRepository;
import br.com.clubedojava.webstore.repository.UserRepository;
import br.com.clubedojava.webstore.service.CartService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Suspendable
    public CompletableFuture<CartDTO> getCart(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCartInVirtualThread(username);
            } catch (Exception e) {
                throw new RuntimeException("Error fetching cart", e);
            }
        });
    }

    private CartDTO getCartInVirtualThread(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createNewCart(user));

        return convertToDTO(cart);
    }

    @Override
    public Optional<Cart> getCartByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return cartRepository.findByUser(user);
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<CartDTO> addItem(String username, CartItemDTO cartItemDTO) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));

            Cart cart = cartRepository.findByUser(user)
                    .orElseGet(() -> createNewCart(user));

            Product product = productRepository.findById(cartItemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartItemDTO.getProductId()));

            // Verifica se o produto já está no carrinho
            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(product.getId()) &&
                            isEqualsOrBothNull(item.getSelectedSize(), cartItemDTO.getSelectedSize()) &&
                            isEqualsOrBothNull(item.getSelectedColor(), cartItemDTO.getSelectedColor()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Atualiza a quantidade
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + cartItemDTO.getQuantity());
                cartItemRepository.save(item);
            } else {
                // Adiciona um novo item
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(cartItemDTO.getQuantity())
                        .selectedSize(cartItemDTO.getSelectedSize())
                        .selectedColor(cartItemDTO.getSelectedColor())
                        .build();

                cart.addCartItem(newItem);
            }

            cartRepository.save(cart);
            return convertToDTO(cart);
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<CartDTO> updateItemQuantity(String username, Long itemId, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));

            Cart cart = cartRepository.findByUser(user)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + username));

            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

            // Verifica se o item pertence ao carrinho do usuário
            if (!cartItem.getCart().getId().equals(cart.getId())) {
                throw new IllegalArgumentException("Cart item does not belong to user's cart");
            }

            // Verifica se o produto tem estoque suficiente
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < quantity) {
                throw new IllegalStateException("Not enough stock for product: " + product.getName());
            }

            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);

            return convertToDTO(cartRepository.findById(cart.getId()).get());
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<CartDTO> removeItem(String username, Long itemId) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));

            Cart cart = cartRepository.findByUser(user)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + username));

            CartItem cartItem = cartItemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

            // Verifica se o item pertence ao carrinho do usuário
            if (!cartItem.getCart().getId().equals(cart.getId())) {
                throw new IllegalArgumentException("Cart item does not belong to user's cart");
            }

            cart.removeCartItem(cartItem);
            cartItemRepository.delete(cartItem);
            cartRepository.save(cart);

            return convertToDTO(cart);
        });
    }

    @Override
    @Transactional
    @Suspendable
    public CompletableFuture<Void> clearCart(Long cartId) {
        return CompletableFuture.runAsync(() -> {
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart createNewCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();

        return cartRepository.save(cart);
    }

    private CartDTO convertToDTO(Cart cart) {
        Set<CartItemDTO> cartItemDTOs = cart.getItems().stream()
                .map(item -> CartItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productImage(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getProduct().getPrice())
                        .selectedSize(item.getSelectedSize())
                        .selectedColor(item.getSelectedColor())
                        .build())
                .collect(Collectors.toSet());

        return CartDTO.builder()
                .id(cart.getId())
                .items(cartItemDTOs)
                .subtotal(cart.getSubtotal())
                .totalItems(cart.getTotalItems())
                .build();
    }

    private boolean isEqualsOrBothNull(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        return s1 != null && s1.equals(s2);
    }
}