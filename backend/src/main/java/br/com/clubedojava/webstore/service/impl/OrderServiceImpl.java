package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.OrderCreateDTO;
import br.com.clubedojava.webstore.dto.OrderDTO;
import br.com.clubedojava.webstore.dto.OrderItemDTO;
import br.com.clubedojava.webstore.dto.OrderUpdateDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.exception.UnauthorizedException;
import br.com.clubedojava.webstore.model.*;
import br.com.clubedojava.webstore.repository.OrderRepository;
import br.com.clubedojava.webstore.repository.ProductRepository;
import br.com.clubedojava.webstore.repository.UserRepository;
import br.com.clubedojava.webstore.service.CartService;
import br.com.clubedojava.webstore.service.OrderService;
import br.com.clubedojava.webstore.service.PaymentService;
import br.com.clubedojava.webstore.service.ShippingService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final ApplicationEventPublisher eventPublisher;
//   CartService
    @Override
    public CompletableFuture<Page<OrderDTO>> findOrdersByUser(String username, Pageable pageable) {
        return null;
    }

    @Override
    public CompletableFuture<Optional<OrderDTO>> findOrderById(Long id, String username) {
        return null;
    }

    @Override
    public CompletableFuture<OrderDTO> createOrder(OrderCreateDTO orderCreateDTO, String username) {
        return null;
    }

    @Override
    public CompletableFuture<OrderDTO> updateOrder(Long id, OrderUpdateDTO orderUpdateDTO, String username) {
        return null;
    }

    @Override
    public CompletableFuture<OrderDTO> cancelOrder(Long id, String username) {
        return null;
    }

    @Override
    public CompletableFuture<Optional<OrderDTO>> findOrderByTrackingNumber(String trackingNumber, String username) {
        return null;
    }
}