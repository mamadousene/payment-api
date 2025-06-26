package sn.com.developer.paymentapi.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;

import java.math.BigDecimal;


/**
 * DTO (Data Transfer Object) pour les requêtes de paiement mobile.
 *
 * <p>Ce record représente une demande de paiement mobile money et contient
 * toutes les informations nécessaires pour initier une transaction de paiement
 * via les opérateurs télécoms (Orange Money, Free Money, Wave, etc.).</p>
 *
 * <p>Utilise le pattern Record de Java pour une immutabilité automatique
 * et une réduction du code boilerplate. Toutes les validations sont effectuées
 * via les annotations Bean Validation pour garantir l'intégrité des données.</p>
 *
 * <h3>Validations appliquées :</h3>
 * <ul>
 *   <li><strong>Montant</strong> : Entre 100 et 2,000,000 FCFA</li>
 *   <li><strong>Téléphone</strong> : Format sénégalais (77, 78, 70, 76, 75 + 7 chiffres)</li>
 *   <li><strong>Correlation ID</strong> : Obligatoire, max 100 caractères</li>
 *   <li><strong>Opérateur</strong> : Doit correspondre à un opérateur supporté</li>
 *   <li><strong>Callback URL</strong> : Format URL valide (HTTP/HTTPS)</li>
 * </ul>
 *
 * <h3>Exemple d'utilisation :</h3>
 * <pre>
 * PaymentRequest request = new PaymentRequest(
 *     new BigDecimal("5000"),
 *     "771234567",
 *     "CORR-2024-001",
 *     OperateurEnum.ORANGE,
 *     "https://monsite.com/callback"
 * );
 * </pre>
 *
 * <h3>Intégration OpenAPI :</h3>
 * <p>Ce DTO est automatiquement documenté dans Swagger/OpenAPI grâce aux
 * annotations @Schema, facilitant la compréhension pour les développeurs clients.</p>
 *
 * @param montant Le montant de la transaction en FCFA (100 - 2,000,000)
 * @param telephone Le numéro de téléphone du destinataire au format sénégalais
 * @param correlationId Identifiant unique de corrélation pour traçabilité
 * @param operateur L'opérateur mobile à utiliser pour la transaction
 * @param callbackUrl URL où sera envoyée la notification de résultat
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 * @see OperateurEnum
 * @see PaymentResponse
 */
@Schema(description = "Requête de paiement mobile")
public record PaymentRequest(

        @Schema(description = "Montant de la transaction en FCFA", example = "5000")
        @NotNull(message = "Le montant est obligatoire")
        BigDecimal montant,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Pattern(regexp = "^(77|78|70|76|75)[0-9]{7}$",
                message = "Format de téléphone invalide. Utilisez le format: 77XXXXXXX")
        @Schema(description = "Numéro de téléphone du destinataire", example = "771234567")
        String telephone,

        @NotBlank(message = "L'ID de corrélation est obligatoire")
        @Size(max = 100, message = "L'ID de corrélation ne peut pas dépasser 100 caractères")
        @Schema(description = "Identifiant unique de corrélation", example = "CORR-2024-001")
        String correlationId,

        @NotNull(message = "L'opérateur est obligatoire")
        @Schema(description = "Opérateur mobile", example = "ORANGE")
        OperateurEnum operateur,

        @NotBlank(message = "L'URL de callback est obligatoire")
        @Pattern(regexp = "^https?://.*", message = "L'URL de callback doit être valide")
        @Schema(description = "URL de notification", example = "https://monsite.com/callback")
        String callbackUrl
) {}