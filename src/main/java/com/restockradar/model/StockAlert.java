package com.restockradar.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts")
@Getter
@Setter
@NoArgsConstructor
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 30)
    private AlertType alertType;

    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    @Column(name = "alert_date")
    private LocalDateTime alertDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AlertStatus status = AlertStatus.Active;

    @PrePersist
    public void prePersist() {
        if (alertDate == null) {
            alertDate = LocalDateTime.now();
        }
    }

    public enum AlertType {
        Low, Overstock
    }

    public enum AlertStatus {
        Active, Resolved
    }
}
