package com.restockradar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ProductForm {
    private Long productId;

    @NotBlank(message = "Product name is required")
    private String productName;

    private Long categoryId;

    @NotNull(message = "Cost price is required")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    private BigDecimal sellingPrice;

    @NotNull(message = "Current stock is required")
    private Integer currentStock;

    @NotNull(message = "Minimum stock is required")
    private Integer minimumStock;

    private String barcode;

    private String batchNumber;

    private Long supplierId;

    private LocalDate expiryDate;
}
