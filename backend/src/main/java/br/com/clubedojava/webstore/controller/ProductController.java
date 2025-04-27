package br.com.clubedojava.webstore.controller;

import br.com.clubedojava.webstore.dto.ProductDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(Pageable pageable) {
        Page<ProductDTO> productPage = productService.findAll(pageable);
        return ResponseEntity.ok(productPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.save(productDTO);
        return ResponseEntity.created(URI.create("/api/products/" + createdProduct.getId()))
                             .body(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO) {
         try {
            ProductDTO updatedProduct = productService.update(id, productDTO);
            return ResponseEntity.ok(updatedProduct);
        } catch (ResourceNotFoundException e) {
             return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
         try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
             return ResponseEntity.notFound().build();
        }
    }

    // TODO: Adicionar endpoints para findByCategory, findFeatured, searchProducts

}
