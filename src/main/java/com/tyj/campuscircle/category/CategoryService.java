package com.tyj.campuscircle.category;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> listEnabledCategories() {
        return categoryMapper.findEnabledCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
