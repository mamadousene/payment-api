package sn.com.developer.paymentapi.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import sn.com.developer.paymentapi.exception.BadRequestAlertException;

/**
 * L'énumération TransactionStatusEnum définit les statuts possibles d'une transaction avec un code et une description,
 * et intègre une gestion personnalisée de la conversion JSON via @JsonValue pour l'affichage du code
 * et @JsonCreator pour la conversion inverse lors de la réception de requêtes.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Getter
public enum TransactionStatusEnum {
    PENDING("PENDING", "Transaction en attente"),
    SUCCESS("SUCCESS", "Transaction réussie"),
    FAILED("FAILED", "Transaction échouée"),
    CANCELLED("CANCELLED", "Transaction annulée"),
    EXPIRED("EXPIRED", "Transaction expirée");

    private final String code;
    private final String description;

    TransactionStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static TransactionStatusEnum fromString(String value) {
        for (TransactionStatusEnum status : TransactionStatusEnum.values()) {
            if (status.code.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BadRequestAlertException(String.format("Statut non supporté: " + value),"","");
    }
}
