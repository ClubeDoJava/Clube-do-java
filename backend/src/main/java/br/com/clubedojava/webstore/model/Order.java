package br.com.clubedojava.webstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String paymentIntentId;

    @Column
    private String trackingNumber;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == OrderStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    // Utilizando Class selada (Java 17 feature)
    public sealed interface OrderEvent permits OrderCreatedEvent, OrderUpdatedEvent, OrderCompletedEvent {
        Long getOrderId();
        LocalDateTime getTimestamp();
    }

    public record OrderCreatedEvent(Long orderId, LocalDateTime timestamp) implements OrderEvent {
        @Override
        public Long getOrderId() {
            return orderId;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    public record OrderUpdatedEvent(Long orderId, OrderStatus oldStatus, OrderStatus newStatus, LocalDateTime timestamp) implements OrderEvent {
        @Override
        public Long getOrderId() {
            return orderId;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    public record OrderCompletedEvent(Long orderId, LocalDateTime timestamp) implements OrderEvent {
        @Override
        public Long getOrderId() {
            return orderId;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
