package sn.com.developer.paymentapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sn.com.developer.paymentapi.domain.enums.CallbackStatusEnum;

import java.time.LocalDateTime;

/**
 * Entité représentant l'historique des callbacks de notification
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Entity
@Table(name = "transaction_callbacks", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_callback_status", columnList = "callbackStatus"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_next_retry_at", columnList = "nextRetryAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TransactionCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="transaction_id",nullable = false, length = 50)
    private String transactionId;

    @Column(nullable = false, length = 500)
    private String callbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallbackStatusEnum callbackStatus;

    @Column
    private Integer responseCode;

    @Column(length = 2000)
    private String responseBody;

    @Column(length = 1000)
    private String responseHeaders;

    @Column(length = 1000)
    private String errorMessage;

    @Builder.Default
    @Column(nullable = false)
    private Integer attemptCount = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxRetryAttempts = 5;

    @Column
    private LocalDateTime nextRetryAt;

    @Column
    private Long executionTimeMs;

    @Column(length = 2000)
    private String requestPayload;

    @Column(length = 500)
    private String requestHeaders;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime failedAt;

    @Version
    private Long version;

    // Relation vers Transaction (clé métier transactionId)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;

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
     * Marque le callback comme réussi
     */
    public void markAsSuccess(Integer responseCode, String responseBody, String responseHeaders, Long executionTime) {
        this.callbackStatus = CallbackStatusEnum.SUCCESS;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
        this.executionTimeMs = executionTime;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.nextRetryAt = null;
        this.errorMessage = null;
    }

    /**
     * Marque le callback comme échoué
     */
    public void markAsFailed(String errorMessage, Integer responseCode, String responseBody, Long executionTime) {
        this.callbackStatus = CallbackStatusEnum.FAILED;
        this.errorMessage = errorMessage;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.executionTimeMs = executionTime;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Planifier le prochain retry si possible
        if (attemptCount < maxRetryAttempts) {
            scheduleNextRetry();
        } else {
            this.callbackStatus = CallbackStatusEnum.EXHAUSTED;
        }
    }

    /**
     * Marque le callback comme en cours
     */
    public void markAsInProgress() {
        this.callbackStatus = CallbackStatusEnum.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de tentatives
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Planifie le prochain retry avec backoff exponentiel
     */
    public void scheduleNextRetry() {
        if (attemptCount < maxRetryAttempts) {
            // Backoff exponentiel: 2^attemptCount minutes
            int delayMinutes = (int) Math.pow(2, attemptCount);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
            this.callbackStatus = CallbackStatusEnum.PENDING_RETRY;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Vérifie si le callback peut être retenté
     */
    public boolean canRetry() {
        return attemptCount < maxRetryAttempts &&
                (callbackStatus == CallbackStatusEnum.FAILED || callbackStatus == CallbackStatusEnum.PENDING_RETRY) &&
                (nextRetryAt == null || LocalDateTime.now().isAfter(nextRetryAt));
    }

    /**
     * Vérifie si le callback est terminé (succès ou épuisé)
     */
    public boolean isCompleted() {
        return callbackStatus == CallbackStatusEnum.SUCCESS ||
                callbackStatus == CallbackStatusEnum.EXHAUSTED;
    }

    /**
     * Initialise un nouveau callback
     */
    public static TransactionCallback createNew(String transactionId, String callbackUrl, String requestPayload, String requestHeaders) {
        return TransactionCallback.builder()
                .transactionId(transactionId)
                .callbackUrl(callbackUrl)
                .callbackStatus(CallbackStatusEnum.PENDING)
                .requestPayload(requestPayload)
                .requestHeaders(requestHeaders)
                .attemptCount(0)
                .maxRetryAttempts(5)
                .build();
    }
}
