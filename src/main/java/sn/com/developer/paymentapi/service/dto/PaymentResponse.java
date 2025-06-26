package sn.com.developer.paymentapi.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;
import sn.com.developer.paymentapi.domain.enums.TransactionStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) pour les réponses de paiement mobile.
 *
 * <p>Ce record représente la réponse retournée après l'initiation d'une transaction
 * de paiement mobile money. Il contient toutes les informations relatives à la
 * transaction créée, incluant son statut et les détails de traçabilité.</p>
 *
 * <p>Cette réponse est générée après le traitement d'une {@link PaymentRequest}
 * et permet au client de suivre l'évolution de sa demande de paiement via
 * l'identifiant de transaction unique.</p>
 *
 * <h3>Cycle de vie d'une transaction :</h3>
 * <ol>
 *   <li><strong>PENDING</strong> : Transaction initiée, en attente de traitement</li>
 *   <li><strong>PROCESSING</strong> : En cours de traitement par l'opérateur</li>
 *   <li><strong>SUCCESS</strong> : Transaction réussie</li>
 *   <li><strong>FAILED</strong> : Transaction échouée</li>
 *   <li><strong>CANCELLED</strong> : Transaction annulée</li>
 * </ol>
 *
 * <h3>Utilisation du callback :</h3>
 * <p>L'URL de callback sera utilisée pour notifier le client du changement
 * de statut de la transaction. Le système enverra une requête POST à cette
 * URL avec les détails de la transaction mise à jour.</p>
 *
 * <h3>Traçabilité :</h3>
 * <ul>
 *   <li><strong>correlationId</strong> : Fourni par le client pour sa propre traçabilité</li>
 *   <li><strong>transactionId</strong> : Généré par le système, unique par transaction</li>
 *   <li><strong>createdAt</strong> : Horodatage précis de création de la transaction</li>
 * </ul>
 *
 * <h3>Exemple de réponse :</h3>
 * <pre>
 * {
 *   "montant": 5000,
 *   "telephone": "771234567",
 *   "correlationId": "CORR-2024-001",
 *   "transactionId": "TXN-20240623-001",
 *   "status": "PENDING",
 *   "operateur": "ORANGE",
 *   "callbackUrl": "https://httpbin.org/post",
 *   "createdAt": "2025-06-25T14:30:00",
 *   "message": "Transaction initiée avec succès"
 * }
 * </pre>
 *
 * <h3>Intégration OpenAPI :</h3>
 * <p>Toutes les propriétés sont documentées avec des exemples pour faciliter
 * l'intégration par les développeurs clients via Swagger UI.</p>
 *
 * @param montant Le montant de la transaction en FCFA
 * @param telephone Le numéro de téléphone du destinataire
 * @param correlationId L'identifiant de corrélation fourni par le client
 * @param transactionId L'identifiant unique de transaction généré par le système
 * @param status Le statut actuel de la transaction
 * @param operateur L'opérateur mobile utilisé pour la transaction
 * @param callbackUrl L'URL où sera envoyée la notification de résultat
 * @param createdAt L'horodatage de création de la transaction
 * @param message Un message descriptif optionnel sur l'état de la transaction
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 * @see PaymentRequest
 * @see TransactionStatusEnum
 * @see OperateurEnum
 */
@Schema(description = "Réponse de transaction de paiement")
public record PaymentResponse(

        @Schema(description = "Montant de la transaction", example = "5000")
        BigDecimal montant,

        @Schema(description = "Numéro de téléphone", example = "771234567")
        String telephone,

        @Schema(description = "ID de corrélation", example = "CORR-2024-001")
        String correlationId,

        @Schema(description = "ID unique de transaction", example = "TXN-20240623-001")
        String transactionId,

        @Schema(description = "Statut de la transaction", example = "PENDING")
        TransactionStatusEnum status,

        @Schema(description = "Opérateur mobile", example = "ORANGE")
        OperateurEnum operateur,

        @Schema(description = "URL de callback", example = "https://mls.sene/callback")
        String callbackUrl,

        @Schema(description = "Horodatage de création", example = "2025-06-25T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Message descriptif", example = "Transaction initiée avec succès",nullable = true)
        String message
) {}