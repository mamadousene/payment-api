package sn.com.developer.paymentapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sn.com.developer.paymentapi.domain.enums.TransactionStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant une transaction de paiement
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_correlation_id", columnList = "correlationId"),
        @Index(name = "idx_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="transaction_id",nullable = false, unique = true, length = 50)
    private String transactionId;

    @Column(nullable = false, length = 100)
    private String correlationId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 15)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatusEnum status;

    @Column(nullable = false, length = 500)
    private String callbackUrl;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime processedAt;

    @Column
    private LocalDateTime notifiedAt;

    @Builder.Default
    @Column
    private Integer retryCount = 0;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operateur_id", nullable = false)
    private Operateur operateur;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Marque la transaction comme traitée
     */
    public void markAsProcessed(TransactionStatusEnum newStatus) {
        this.status = newStatus;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marque la transaction comme notifiée
     */
    public void markAsNotified() {
        this.notifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de tentatives
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
}