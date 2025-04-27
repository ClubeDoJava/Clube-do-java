package br.com.clubedojava.webstore.repository;

import br.com.clubedojava.webstore.model.Cart;
import br.com.clubedojava.webstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}