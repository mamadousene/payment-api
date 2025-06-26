package sn.com.developer.paymentapi.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Exception personnalisée pour les requêtes malformées avec support des détails d'erreur.
 * Exception levée pour indiquer une requête malformée ou invalide.
 * Fournit des détails contextuels pour faciliter le debugging et l'expérience utilisateur.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public final class BadRequestAlertException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5342685348889292116L;

    private final String entityName;
    private final String errorKey;
    private final Map<String, Object> errorDetails;
    private final Instant timestamp;
    @Getter
    private final String requestId;

    /**
     * Constructeur principal avec message, entité et clé d'erreur.
     *
     * @param message Message d'erreur descriptif
     * @param entityName Nom de l'entité concernée
     * @param errorKey Clé d'erreur pour l'internationalisation
     */
    public BadRequestAlertException(String message, String entityName, String errorKey) {
        this(message, entityName, errorKey, null, null);
    }

    /**
     * Constructeur avec détails d'erreur supplémentaires.
     *
     * @param message Message d'erreur descriptif
     * @param entityName Nom de l'entité concernée
     * @param errorKey Clé d'erreur pour l'internationalisation
     * @param errorDetails Détails supplémentaires de l'erreur
     */
    public BadRequestAlertException(String message, String entityName, String errorKey,
                                    Map<String, Object> errorDetails) {
        this(message, entityName, errorKey, errorDetails, null);
    }

    /**
     * Constructeur complet avec cause racine.
     *
     * @param message Message d'erreur descriptif
     * @param entityName Nom de l'entité concernée
     * @param errorKey Clé d'erreur pour l'internationalisation
     * @param errorDetails Détails supplémentaires de l'erreur
     * @param cause Exception racine
     */
    public BadRequestAlertException(String message, String entityName, String errorKey,
                                    Map<String, Object> errorDetails, Throwable cause) {
        super(Objects.requireNonNull(message, "Message cannot be null"), cause);
        this.entityName = Objects.requireNonNull(entityName, "Entity name cannot be null");
        this.errorKey = Objects.requireNonNull(errorKey, "Error key cannot be null");
        this.errorDetails = errorDetails != null ? Map.copyOf(errorDetails) : Collections.emptyMap();
        this.timestamp = Instant.now();
        this.requestId = generateRequestId();
    }


    /**
     * Vérifie si des détails d'erreur sont présents.
     */
    public boolean hasErrorDetails() {
        return !errorDetails.isEmpty();
    }


    /**
     * Builder pattern pour une construction fluide.
     */
    public static Builder builder(String message, String entityName, String errorKey) {
        return new Builder(message, entityName, errorKey);
    }

    /**
     * Génère un ID unique pour tracer la requête.
     */
    @JsonIgnore
    private String generateRequestId() {
        return "REQ_" + System.nanoTime() + "_" + Thread.currentThread().getId();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BadRequestAlertException.class.getSimpleName() + "[", "]")
                .add("message='" + getMessage() + "'")
                .add("entityName='" + entityName + "'")
                .add("errorKey='" + errorKey + "'")
                .add("requestId='" + requestId + "'")
                .add("timestamp=" + timestamp)
                .add("hasDetails=" + hasErrorDetails())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BadRequestAlertException that = (BadRequestAlertException) obj;
        return Objects.equals(getMessage(), that.getMessage()) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(errorKey, that.errorKey) &&
                Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMessage(), entityName, errorKey, requestId);
    }

    /**
     * Builder pour construction fluide de l'exception.
     */
    public static final class Builder {
        private final String message;
        private final String entityName;
        private final String errorKey;
        private Map<String, Object> errorDetails;
        private Throwable cause;

        private Builder(String message, String entityName, String errorKey) {
            this.message = message;
            this.entityName = entityName;
            this.errorKey = errorKey;
        }

        public Builder withDetails(Map<String, Object> details) {
            this.errorDetails = details;
            return this;
        }

        public Builder withDetail(String key, Object value) {
            if (this.errorDetails == null) {
                this.errorDetails = new java.util.HashMap<>();
            }
            this.errorDetails.put(key, value);
            return this;
        }

        public Builder causedBy(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public BadRequestAlertException build() {
            return new BadRequestAlertException(message, entityName, errorKey, errorDetails, cause);
        }
    }

    public static BadRequestAlertException invalidField(String fieldName, Object value) {
        return builder("Invalid field value", "Field", "validation.invalid")
                .withDetail("field", fieldName)
                .withDetail("value", value)
                .build();
    }

    public static BadRequestAlertException missingField(String fieldName) {
        return builder("Required field is missing", "Field", "validation.required")
                .withDetail("field", fieldName)
                .build();
    }

    public static BadRequestAlertException duplicateEntity(String entityName, String identifier) {
        return builder("Entity already exists", entityName, "entity.duplicate")
                .withDetail("identifier", identifier)
                .build();
    }
}
