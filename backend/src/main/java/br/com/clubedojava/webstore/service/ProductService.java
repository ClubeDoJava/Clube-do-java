package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductService {

    Page<ProductDTO> findAll(Pageable pageable);

    Optional<ProductDTO> findById(Long id);

    ProductDTO save(ProductDTO productDTO);

    ProductDTO update(Long id, ProductDTO productDTO);

    void delete(Long id);

    Page<ProductDTO> findByCategory(Long categoryId, Pageable pageable);

    Page<ProductDTO> findFeatured(Pageable pageable);

    Page<ProductDTO> searchProducts(String keyword, Pageable pageable);
}