package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.OrderCreateDTO;
import br.com.clubedojava.webstore.dto.OrderDTO;
import br.com.clubedojava.webstore.dto.OrderUpdateDTO;
import br.com.clubedojava.webstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public CompletableFuture<ResponseEntity<Page<OrderDTO>>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return orderService.findOrdersByUser(userDetails.getUsername(), pageable)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<OrderDTO>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return orderService.findOrderById(id, userDetails.getUsername())
                .thenApply(orderOpt -> orderOpt
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<OrderDTO>> createOrder(
            @Valid @RequestBody OrderCreateDTO orderCreateDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        return orderService.createOrder(orderCreateDTO, userDetails.getUsername())
                .thenApply(order -> new ResponseEntity<>(order, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<OrderDTO>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateDTO orderUpdateDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        return orderService.updateOrder(id, orderUpdateDTO, userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @PatchMapping("/{id}/cancel")
    public CompletableFuture<ResponseEntity<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return orderService.cancelOrder(id, userDetails.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public CompletableFuture<ResponseEntity<OrderDTO>> getOrderByTrackingNumber(
            @PathVariable String trackingNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return orderService.findOrderByTrackingNumber(trackingNumber, userDetails.getUsername())
                .thenApply(orderOpt -> orderOpt
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }
}
