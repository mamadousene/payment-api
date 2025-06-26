package sn.com.developer.paymentapi.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Énumération des opérateurs mobiles supportés au Sénégal
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Getter
public enum OperateurEnum {
    ORANGE("ORANGE", "Orange Money", "77", "78"),
    EXPRESSO("EXPRESSO", "Expresso Mobile Money", "70"),
    YAS("YAS", "YAS Mobile Money", "76", "75");

    private final String code;
    private final String displayName;
    private final String[] prefixes;

    OperateurEnum(String code, String displayName, String... prefixes) {
        this.code = code;
        this.displayName = displayName;
        this.prefixes = prefixes;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static OperateurEnum fromString(String value) {
        for (OperateurEnum operateur : OperateurEnum.values()) {
            if (operateur.code.equalsIgnoreCase(value)) {
                return operateur;
            }
        }
        throw new IllegalArgumentException("Opérateur non supporté: " + value +
                ". Opérateurs valides: ORANGE, EXPRESSO, YAS");
    }

    /**
     * Détermine l'opérateur basé sur le préfixe du numéro de téléphone
     */
    public static OperateurEnum fromPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 2) {
            throw new IllegalArgumentException("Numéro de téléphone invalide");
        }

        String prefix = phoneNumber.substring(0, 2);

        for (OperateurEnum operateur : values()) {
            for (String p : operateur.prefixes) {
                if (p.equals(prefix)) {
                    return operateur;
                }
            }
        }
        throw new IllegalArgumentException("Préfixe non reconnu: " + prefix);
    }
}