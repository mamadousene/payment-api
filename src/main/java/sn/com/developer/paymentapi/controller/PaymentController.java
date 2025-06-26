package sn.com.developer.paymentapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.com.developer.paymentapi.service.PaymentService;
import sn.com.developer.paymentapi.service.dto.PaymentRequest;
import sn.com.developer.paymentapi.service.dto.PaymentResponse;

/**
 * Contrôleur REST pour les opérations de paiement mobile
 * Gère les transactions ORANGE, EXPRESSO, YAS
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@RestController
@RequestMapping("/api/v1/payment-api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment API", description = "API de paiement mobile pour opérateurs sénégalais")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Endpoint pour initier un paiement 
     *
     * @param apiKey Clé API pour l'authentification
     * @param request Données de la requête de paiement
     * @return Réponse avec les détails de la transaction
     */
    @PostMapping("/envoi")
    @Operation(
            summary = "Initier un paiement mobile",
            description = "Crée une nouvelle transaction de paiement pour les opérateurs ORANGE, EXPRESSO ou YAS",
            security = @SecurityRequirement(name = "API-KEY")
    )
    @ApiResponse(responseCode = "200", description = "Transaction créée avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "401", description = "API Key invalide")
    @ApiResponse(responseCode = "500", description = "Erreur serveur")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Parameter(description = "Clé API", required = true)
            @RequestHeader("API-KEY") String apiKey,

            @Parameter(description = "Données de paiement", required = true)
            @Valid @RequestBody PaymentRequest request) {

        log.info("Réception demande paiement - Correlation ID: {}, Montant: {}, Opérateur: {}",
                request.correlationId(), request.montant(), request.operateur());

        try {
            PaymentResponse response = paymentService.processPayment(request, apiKey);

            log.info("Transaction créée - Transaction ID: {}, Status: {}",
                    response.transactionId(), response.status());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du traitement du paiement - Correlation ID: {}",
                    request.correlationId(), e);
            throw e;
        }
    }
}
