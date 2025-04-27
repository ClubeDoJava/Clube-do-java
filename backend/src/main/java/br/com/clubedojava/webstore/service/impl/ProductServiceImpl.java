package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.ProductDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.model.Category;
import br.com.clubedojava.webstore.model.Product;
import br.com.clubedojava.webstore.repository.CategoryRepository;
import br.com.clubedojava.webstore.repository.ProductRepository;
import br.com.clubedojava.webstore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDTO> findById(Long id) {
        return productRepository.findById(id).map(this::convertToDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDTO save(ProductDTO productDTO) {
        Product product = convertToEntity(productDTO);
        product.setId(null);
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductDTO update(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        updateProductFromDTO(existingProduct, productDTO);
        existingProduct.setId(id);
        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDto(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'category_' + #categoryId")
    public Page<ProductDTO> findByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
             throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId, pageable).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "featuredProducts")
    public Page<ProductDTO> findFeatured(Pageable pageable) {
        return productRepository.findByFeaturedTrue(pageable).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable).map(this::convertToDto);
    }

    private ProductDTO convertToDto(Product product) {
        if (product == null) {
            return null;
        }
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .additionalImages(product.getAdditionalImages())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .featured(product.getFeatured())
                .availableSizes(product.getAvailableSizes())
                .availableColors(product.getAvailableColors())
                .weight(product.getWeight())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Product convertToEntity(ProductDTO productDTO) {
        Product product = new Product();
        updateProductFromDTO(product, productDTO);
        return product;
    }

    private void updateProductFromDTO(Product product, ProductDTO productDTO) {
        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getStockQuantity() != null) {
            product.setStockQuantity(productDTO.getStockQuantity());
        }
        if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl());
        }
        if (productDTO.getAdditionalImages() != null) {
            product.setAdditionalImages(productDTO.getAdditionalImages());
        }
        if (productDTO.getFeatured() != null) {
            product.setFeatured(productDTO.getFeatured());
        }
        if (productDTO.getAvailableSizes() != null) {
            product.setAvailableSizes(productDTO.getAvailableSizes());
        }
        if (productDTO.getAvailableColors() != null) {
            product.setAvailableColors(productDTO.getAvailableColors());
        }
        if (productDTO.getWeight() != null) {
            product.setWeight(productDTO.getWeight());
        }
        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
    }
}