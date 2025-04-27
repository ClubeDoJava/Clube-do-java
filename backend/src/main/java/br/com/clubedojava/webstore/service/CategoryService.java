package br.com.clubedojava.webstore.service;

import br.com.clubedojava.webstore.dto.CategoryDTO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<CategoryDTO> findAll();

    Optional<CategoryDTO> findById(Long id);

    CategoryDTO save(CategoryDTO categoryDTO);

    CategoryDTO update(Long id, CategoryDTO categoryDTO);

    void delete(Long id);

    List<CategoryDTO> findByParentId(Long parentId);

    List<CategoryDTO> findAllParentCategories();
}