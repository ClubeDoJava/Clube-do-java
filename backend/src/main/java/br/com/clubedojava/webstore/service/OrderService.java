package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.OrderCreateDTO;
import br.com.clubedojava.webstore.dto.OrderDTO;
import br.com.clubedojava.webstore.dto.OrderUpdateDTO;
import br.com.clubedojava.webstore.service.impl.Suspendable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OrderService {

    @Suspendable
    CompletableFuture<Page<OrderDTO>> findOrdersByUser(String username, Pageable pageable);

    @Suspendable
    CompletableFuture<Optional<OrderDTO>> findOrderById(Long id, String username);

    @Suspendable
    CompletableFuture<OrderDTO> createOrder(OrderCreateDTO orderCreateDTO, String username);

    @Suspendable
    CompletableFuture<OrderDTO> updateOrder(Long id, OrderUpdateDTO orderUpdateDTO, String username);

    @Suspendable
    CompletableFuture<OrderDTO> cancelOrder(Long id, String username);

    @Suspendable
    CompletableFuture<Optional<OrderDTO>> findOrderByTrackingNumber(String trackingNumber, String username);
}