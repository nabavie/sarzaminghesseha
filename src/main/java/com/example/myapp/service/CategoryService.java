package com.example.myapp.service;

import com.example.myapp.model.Category;
import com.example.myapp.repository.CategoryRepository;
import com.example.myapp.repository.TaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TaleRepository taleRepository;

    public CategoryService(CategoryRepository categoryRepository, TaleRepository taleRepository) {
        this.categoryRepository = categoryRepository;
        this.taleRepository = taleRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public boolean nameExists(String name) {
        return categoryRepository.findByName(name.trim()).isPresent();
    }

    @Transactional
    public Category create(String name) {
        return categoryRepository.save(new Category(name.trim()));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.findById(id).ifPresent(category -> {
            // detach from tales first so the join-table rows don't block the delete
            taleRepository.findByCategoriesContaining(category)
                    .forEach(tale -> tale.getCategories().remove(category));
            categoryRepository.delete(category);
        });
    }
}
