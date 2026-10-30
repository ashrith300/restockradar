package com.restockradar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class StockIntakeForm {

    @NotNull(message = "Please select a product")
    private Long productId;

    private Long supplierId;

    @NotNull(message = "Quantity received is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityReceived;

    private BigDecimal purchaseUnitPrice;

    private String batchNumber;

    private LocalDate expiryDate;

    private String invoiceNumber;

    private String notes;
}

