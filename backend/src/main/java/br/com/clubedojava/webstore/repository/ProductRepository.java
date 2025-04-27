package br.com.clubedojava.webstore.repository;

import br.com.clubedojava.webstore.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByFeaturedTrue(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.category c WHERE " +
            "c.id = :categoryId OR c.parent.id = :categoryId")
    Page<Product> findByCategoryIdIncludingChildren(@Param("categoryId") Long categoryId, Pageable pageable);
}