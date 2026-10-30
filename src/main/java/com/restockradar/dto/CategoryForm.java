package com.restockradar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryForm {
    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;
}
