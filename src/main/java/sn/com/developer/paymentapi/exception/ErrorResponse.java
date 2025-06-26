package sn.com.developer.paymentapi.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;
import java.util.List;

/**
 * ErrorResponse représente une réponse d'erreur renvoyée par l'API en cas d'échec ou d'erreur.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    /**
     * Horodatage de l'erreur.
     */
    Timestamp timestamp;

    /**
     * Statut HTTP de l'erreur.
     */
    int status;

    /**
     * Description de l'erreur.
     */
    String error;

    /**
     * Liste de messages détaillant l'erreur.
     */
    List<String> messages;

    /**
     * Chemin de l'URL de la requête qui a provoqué l'erreur.
     */
    String path;
}

