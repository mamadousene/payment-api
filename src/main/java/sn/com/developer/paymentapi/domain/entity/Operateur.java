package sn.com.developer.paymentapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entité représentant un opérateur télécom
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Entity
@Table(name = "operateurs", indexes = {
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_is_active", columnList = "isActive"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Operateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private OperateurEnum code;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 500)
    private String apiEndpoint;

    @Column(length = 100)
    private String apiKey;

    @Column(length = 255)
    private String apiSecret;

    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private Integer timeoutSeconds = 30;

    @Builder.Default
    @Column(nullable = false)
    private Integer maxRetry = 3;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal montantMax;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal commission = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 10)
    private String currency = "XOF";

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String webhookUrl;

    @Column(length = 100)
    private String webhookSecret;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastUsedAt;

    @Column
    private LocalDateTime deactivatedAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalTransactions = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer successfulTransactions = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer failedTransactions = 0;

    @Version
    private Long version;

    // Relations
    @OneToMany(mappedBy = "operateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    // Préfixes stockés dans table à part via ElementCollection
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "operateur_prefixes", joinColumns = @JoinColumn(name = "operateur_id"))
    @Column(name = "prefix")
    @Builder.Default
    private Set<String> prefixes = new HashSet<>();

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
     * Valide si un numéro de téléphone est supporté par cet opérateur
     */
    public boolean isPhoneNumberSupported(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 2) {
            return false;
        }

        String phonePrefix = phoneNumber.substring(0, 2);
        return prefixes.contains(phonePrefix.trim());
    }

    /**
     * Valide si un montant est dans la plage autorisée
     */
    public boolean isMontantValid(BigDecimal montant) {
        if (montant == null) {
            return false;
        }
        boolean minValid = montantMin == null || montant.compareTo(montantMin) >= 0;
        boolean maxValid = montantMax == null || montant.compareTo(montantMax) <= 0;
        return minValid && maxValid;
    }

    /**
     * Met à jour la dernière utilisation
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de transactions totales
     */
    public void incrementTotalTransactions() {
        this.totalTransactions++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de transactions réussies
     */
    public void incrementSuccessfulTransactions() {
        this.successfulTransactions++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de transactions échouées
     */
    public void incrementFailedTransactions() {
        this.failedTransactions++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Désactive l'opérateur
     */
    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Active l'opérateur
     */
    public void activate() {
        this.isActive = true;
        this.deactivatedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calcule le taux de réussite
     */
    public double getSuccessRate() {
        if (totalTransactions == 0) {
            return 0.0;
        }
        return (double) successfulTransactions / totalTransactions * 100;
    }

    /**
     * Retourne la liste des préfixes supportés (en List pour l’API)
     */
    public List<String> getSupportedPrefixes() {
        return new ArrayList<>(prefixes);
    }
}
