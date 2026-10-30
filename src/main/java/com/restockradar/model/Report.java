package com.restockradar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "revenue", precision = 10, scale = 2)
    private BigDecimal revenue;

    @Column(name = "top_selling")
    private Boolean topSelling = false;

    @Column(name = "dead_stock")
    private Boolean deadStock = false;

    @Column(name = "generated_on")
    private LocalDateTime generatedOn;

    @PrePersist
    public void prePersist() {
        if (generatedOn == null) {
            generatedOn = LocalDateTime.now();
        }
    }
}
