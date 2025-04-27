package br.com.clubedojava.webstore.dto;

import br.com.clubedojava.webstore.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private Set<OrderItemDTO> items = new HashSet<>();
    private BigDecimal totalAmount;
    private OrderStatus status;
    private AddressDTO shippingAddress;
    private AddressDTO billingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String trackingNumber;
}