package com.restockradar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reorder_recommendations")
@Getter
@Setter
@NoArgsConstructor
public class ReorderRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reorder_id")
    private Long reorderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "average_daily_sales", precision = 10, scale = 2)
    private BigDecimal averageDailySales;

    @Column(name = "days_to_stockout")
    private Integer daysToStockout;

    @Column(name = "suggested_quantity")
    private Integer suggestedQuantity;

    @Column(name = "reorder_date")
    private LocalDate reorderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", length = 20)
    private RecommendationStatus recommendationStatus = RecommendationStatus.Pending;

    public enum RecommendationStatus {
        Pending, Ordered
    }
}
