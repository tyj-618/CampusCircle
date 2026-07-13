package com.tyj.campuscircle.category;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        Integer sortOrder
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.id(),
                category.name(),
                category.code(),
                category.sortOrder()
        );
    }
}
