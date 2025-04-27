package br.com.clubedojava.webstore.repository;

import br.com.clubedojava.webstore.model.Order;
import br.com.clubedojava.webstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser(User user, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByTrackingNumber(String trackingNumber);
    Optional<Order> findByPaymentIntentId(String paymentIntentId);
}