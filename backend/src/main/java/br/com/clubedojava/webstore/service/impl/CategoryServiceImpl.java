package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.CategoryDTO;
import br.com.clubedojava.webstore.exception.ResourceNotFoundException;
import br.com.clubedojava.webstore.model.Category;
import br.com.clubedojava.webstore.repository.CategoryRepository;
import br.com.clubedojava.webstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final String IMAGE_BASE_PATH = "assets/images/categories/"; // Define o caminho base

    @Override
    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public Optional<CategoryDTO> findById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO save(CategoryDTO categoryDTO) {
        Category category = convertToEntity(categoryDTO);
        category.setId(null); // Garante inserção
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryDTO update(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        updateCategoryFromDTO(existingCategory, categoryDTO);
        existingCategory.setId(id); // Garante o ID
        Category updatedCategory = categoryRepository.save(existingCategory);
        return convertToDto(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        // TODO: Verificar se a categoria tem produtos associados antes de deletar?
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> findByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllParentCategories() {
         return categoryRepository.findByParentIdIsNull().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // --- Métodos de Mapeamento ---

    private CategoryDTO convertToDto(Category category) {
        if (category == null) {
            return null;
        }
        String imageUrl = category.getImageUrl();
        if (imageUrl != null && !imageUrl.startsWith("http") && !imageUrl.startsWith("assets/")) {
            imageUrl = IMAGE_BASE_PATH + imageUrl;
        }

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(imageUrl)
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }

    private Category convertToEntity(CategoryDTO categoryDTO) {
        Category category = new Category();
        updateCategoryFromDTO(category, categoryDTO);
        return category;
    }

    private void updateCategoryFromDTO(Category category, CategoryDTO categoryDTO) {
        if (categoryDTO.getName() != null) {
            category.setName(categoryDTO.getName());
        }
        if (categoryDTO.getDescription() != null) {
            category.setDescription(categoryDTO.getDescription());
        }
        if (categoryDTO.getImageUrl() != null) {
             String imageUrl = categoryDTO.getImageUrl();
             if (imageUrl.startsWith(IMAGE_BASE_PATH)) {
                 imageUrl = imageUrl.substring(IMAGE_BASE_PATH.length());
             }
             category.setImageUrl(imageUrl);
        }
        if (categoryDTO.getParentId() != null) { 
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + categoryDTO.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }
}