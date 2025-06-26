package sn.com.developer.paymentapi.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.com.developer.paymentapi.config.PaymentProperties;
import sn.com.developer.paymentapi.domain.entity.Operateur;
import sn.com.developer.paymentapi.domain.entity.Transaction;
import sn.com.developer.paymentapi.domain.entity.TransactionCallback;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;
import sn.com.developer.paymentapi.domain.enums.TransactionStatusEnum;
import sn.com.developer.paymentapi.exception.BadRequestAlertException;
import sn.com.developer.paymentapi.repository.OperateurRepository;
import sn.com.developer.paymentapi.repository.TransactionRepository;
import sn.com.developer.paymentapi.service.PaymentService;
import sn.com.developer.paymentapi.service.TransactionCallbackService;
import sn.com.developer.paymentapi.service.dto.PaymentRequest;
import sn.com.developer.paymentapi.service.dto.PaymentResponse;
import sn.com.developer.paymentapi.utils.TransactionIdGenerator;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * PaymentServiceImpl contient la logique métier principale liée au traitement des paiements, comme la validation des requêtes,
 * l’appel aux opérateurs, la sauvegarde des transactions, et le déclenchement des callbacks.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OperateurRepository operateurRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionIdGenerator transactionIdGenerator;
    private final TransactionCallbackService transactionCallbackService;
    private final PaymentProperties paymentProperties;

    @Qualifier("payment-async-task-executor")
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Traite une demande de paiement de manière synchrone avec validation et déclenchement asynchrone.
     * Valide la clé API, vérifie l'opérateur, crée la transaction et lance le traitement asynchrone.
     *
     * @param request La demande de paiement contenant les détails de la transaction
     * @param apiKey La clé API pour l'authentification
     * @return PaymentResponse contenant les détails de la transaction créée
     * @throws BadRequestAlertException Si la clé API ou l'opérateur est invalide
     */
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String apiKey) {
        validateApiKey(apiKey);

        var operateur = findOperateurByCode(request.operateur());
        var transaction = createPendingTransaction(request, operateur);

        transactionRepository.save(transaction);
        log.info("Transaction PENDING created with ID={}", transaction.getTransactionId());

        processTransactionAsync(transaction);

        return buildPaymentResponse(transaction, request);
    }

    /**
     * Valide la clé API fournie en la comparant avec la configuration.
     *
     * @param apiKey La clé API à valider
     * @throws BadRequestAlertException Si la clé API est invalide
     */
    private void validateApiKey(String apiKey) {
        var configuredApiKey = paymentProperties.getApi().getKey();

        if (!configuredApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempted: {}", apiKey);
            throw new BadRequestAlertException("Clé API invalide", "Client", "apiKey.invalid");
        }
    }

    /**
     * Recherche un opérateur par son code et lève une exception si non trouvé.
     *
     * @param operateurCode Le code de l'opérateur à rechercher
     * @return L'opérateur correspondant au code
     * @throws BadRequestAlertException Si l'opérateur n'est pas supporté
     */
    private Operateur findOperateurByCode(OperateurEnum operateurCode) {
        return operateurRepository.findByCode(operateurCode)
                .orElseThrow(() -> {
                    log.warn("Unsupported operator code: {}", operateurCode);
                    return new BadRequestAlertException("Opérateur non supporté", "Operateur", "operateur.invalid");
                });
    }

    /**
     * Crée une nouvelle transaction avec le statut PENDING.
     *
     * @param request La demande de paiement
     * @param operateur L'opérateur validé
     * @return La transaction créée avec un ID généré
     */
    private Transaction createPendingTransaction(PaymentRequest request, Operateur operateur) {
        var transactionId = transactionIdGenerator.generateWithPrefix(operateur.getCode().getCode());

        return Transaction.builder()
                .transactionId(transactionId)
                .correlationId(request.correlationId())
                .montant(request.montant())
                .telephone(request.telephone())
                .status(TransactionStatusEnum.PENDING)
                .callbackUrl(request.callbackUrl())
                .operateur(operateur)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Lance le traitement asynchrone de la transaction via l'executor configuré.
     *
     * @param transaction La transaction à traiter de manière asynchrone
     */
    private void processTransactionAsync(Transaction transaction) {
        CompletableFuture
                .runAsync(() -> sendToOperateurAsync(transaction), taskExecutor)
                .exceptionally(throwable -> {
                    log.error("Async processing failed for transactionId={}",
                            transaction.getTransactionId(), throwable);
                    return null;
                });
    }

    /**
     * Construit la réponse de paiement à partir de la transaction créée.
     *
     * @param transaction La transaction créée
     * @param request La demande de paiement originale
     * @return La réponse de paiement formatée
     */
    private PaymentResponse buildPaymentResponse(Transaction transaction, PaymentRequest request) {
        return new PaymentResponse(
                transaction.getMontant(),
                transaction.getTelephone(),
                transaction.getCorrelationId(),
                transaction.getTransactionId(),
                transaction.getStatus(),
                request.operateur(),
                transaction.getCallbackUrl(),
                transaction.getCreatedAt(),
                "Transaction initiée avec succès"
        );
    }

    /**
     * Traite l'envoi de la transaction vers l'opérateur de manière asynchrone.
     * Met à jour le statut de la transaction selon le résultat et déclenche le callback si nécessaire.
     *
     * @param transaction La transaction à envoyer vers l'opérateur
     */
    private void sendToOperateurAsync(Transaction transaction) {
        log.info("Sending to operator async - transactionId={}, correlationId={}",
                transaction.getTransactionId(), transaction.getCorrelationId());

        var operatorResult = callOperateurSafely(transaction);

        operatorResult.ifPresentOrElse(
                success -> handleOperatorResponse(transaction, success),
                () -> handleOperatorException(transaction, new RuntimeException("Unknown operator error"))
        );
    }

    /**
     * Appelle l'opérateur de manière sécurisée avec gestion d'exception.
     *
     * @param transaction La transaction à envoyer
     * @return Optional contenant le résultat (true/false) ou vide en cas d'exception
     */
    private Optional<Boolean> callOperateurSafely(Transaction transaction) {
        try {
            var success = callOperateur(transaction);
            return Optional.of(success);
        } catch (Exception ex) {
            log.error("Exception during operator call for transactionId={}",
                    transaction.getTransactionId(), ex);
            handleOperatorException(transaction, ex);
            return Optional.empty();
        }
    }

    /**
     * Gère la réponse de l'opérateur en mettant à jour le statut de la transaction.
     *
     * @param transaction La transaction traitée
     * @param success Le résultat de l'appel opérateur (true = succès, false = échec)
     */
    private void handleOperatorResponse(Transaction transaction, boolean success) {
        var newStatus = success ? TransactionStatusEnum.SUCCESS : TransactionStatusEnum.FAILED;

        updateTransactionStatus(transaction, newStatus);

        if (success) {
            log.info("Transaction SUCCESS updated, ID={}", transaction.getTransactionId());
            triggerCallbackIfConfigured(transaction);
        } else {
            log.error("Transaction FAILED by operator, ID={}", transaction.getTransactionId());
        }
    }

    /**
     * Gère les exceptions survenues lors de l'appel opérateur.
     *
     * @param transaction La transaction en erreur
     * @param exception L'exception survenue
     */
    private void handleOperatorException(Transaction transaction, Exception exception) {
        updateTransactionStatus(transaction, TransactionStatusEnum.FAILED);
        log.error("Operator call failed for transactionId={}", transaction.getTransactionId(), exception);
    }

    /**
     * Met à jour le statut d'une transaction et la sauvegarde en base.
     *
     * @param transaction La transaction à mettre à jour
     * @param status Le nouveau statut à appliquer
     */
    private void updateTransactionStatus(Transaction transaction, TransactionStatusEnum status) {
        transaction.setStatus(status);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    /**
     * Déclenche le callback si une URL de callback est configurée pour la transaction.
     *
     * @param transaction La transaction pour laquelle déclencher le callback
     */
    private void triggerCallbackIfConfigured(Transaction transaction) {
        Optional.ofNullable(transaction.getCallbackUrl())
                .filter(url -> !url.isBlank())
                .ifPresent(url -> {
                    var callback = createTransactionCallback(transaction);
                    transactionCallbackService.triggerCallbackAsync(callback);
                });
    }

    /**
     * Simule l'appel vers l'opérateur de paiement.
     * En production, cette méthode ferait un appel HTTP/API réel vers l'opérateur.
     *
     * @param transaction La transaction à traiter par l'opérateur
     * @return true si l'opérateur accepte le paiement, false sinon
     */
    private boolean callOperateur(Transaction transaction) {
        log.debug("Simulating operator call: transactionId={}, amount={}, phone={}",
                transaction.getTransactionId(), transaction.getMontant(), transaction.getTelephone());


        /* RestTemplate implementation for real operator call:
         *
         * Operateur operateur = transaction.getOperateur();
         *
         * // Construire les headers d'authentification
         * HttpHeaders headers = new HttpHeaders();
         * headers.setContentType(MediaType.APPLICATION_JSON);
         *
         * // Authentification selon le type d'opérateur
         * if (operateur.getApiKey() != null) {
         *     headers.set("Authorization", "Bearer " + operateur.getApiKey());
         * } else if (operateur.getUsername() != null && operateur.getPassword() != null) {
         *     String auth = operateur.getUsername() + ":" + operateur.getPassword();
         *     String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
         *     headers.set("Authorization", "Basic " + encodedAuth);
         * }
         *
         * // Construire le payload de la requête
         * Map<String, Object> requestBody = new HashMap<>();
         * requestBody.put("transactionId", transaction.getTransactionId());
         * requestBody.put("amount", transaction.getMontant());
         * requestBody.put("currency", operateur.getCurrency());
         * requestBody.put("phoneNumber", transaction.getTelephone());
         * requestBody.put("reference", transaction.getReference());
         * requestBody.put("webhookUrl", operateur.getWebhookUrl());
         *
         * HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
         *
         * try {
         *     // Configuration du timeout
         *     RestTemplate restTemplate = new RestTemplate();
         *     restTemplate.getRequestFactory().setConnectTimeout(operateur.getTimeoutSeconds() * 1000);
         *     restTemplate.getRequestFactory().setReadTimeout(operateur.getTimeoutSeconds() * 1000);
         *
         *     // Appel vers l'endpoint de l'opérateur
         *     ResponseEntity<Map> response = restTemplate.postForEntity(
         *         operateur.getApiEndpoint() + "/payment/initiate",
         *         request,
         *         Map.class
         *     );
         *
         *     // Traitement de la réponse
         *     if (response.getStatusCode() == HttpStatus.OK) {
         *         Map<String, Object> responseBody = response.getBody();
         *         String status = (String) responseBody.get("status");
         *
         *         // Mise à jour des statistiques de l'opérateur
         *         operateur.updateLastUsed();
         *         operateur.incrementTotalTransactions();
         *
         *         if ("SUCCESS".equals(status) || "ACCEPTED".equals(status)) {
         *             operateur.incrementSuccessfulTransactions();
         *             return true;
         *         } else {
         *             operateur.incrementFailedTransactions();
         *             log.warn("Operator rejected payment: status={}, transactionId={}",
         *                     status, transaction.getTransactionId());
         *             return false;
         *         }
         *     } else {
         *         operateur.incrementFailedTransactions();
         *         log.error("Operator call failed: statusCode={}, transactionId={}",
         *                 response.getStatusCode(), transaction.getTransactionId());
         *         return false;
         *     }
         *
         * } catch (ResourceAccessException e) {
         *     // Timeout ou problème de connexion
         *     operateur.incrementFailedTransactions();
         *     log.error("Operator timeout/connection error: transactionId={}, error={}",
         *             transaction.getTransactionId(), e.getMessage());
         *     return false;
         *
         * } catch (HttpClientErrorException e) {
         *     // Erreur 4xx (Bad Request, Unauthorized, etc.)
         *     operateur.incrementFailedTransactions();
         *     log.error("Operator client error: statusCode={}, transactionId={}, response={}",
         *             e.getStatusCode(), transaction.getTransactionId(), e.getResponseBodyAsString());
         *     return false;
         *
         * } catch (HttpServerErrorException e) {
         *     // Erreur 5xx côté opérateur
         *     operateur.incrementFailedTransactions();
         *     log.error("Operator server error: statusCode={}, transactionId={}",
         *             e.getStatusCode(), transaction.getTransactionId());
         *     return false;
         *
         * } catch (Exception e) {
         *     // Autres erreurs
         *     operateur.incrementFailedTransactions();
         *     log.error("Unexpected error calling operator: transactionId={}, error={}",
         *             transaction.getTransactionId(), e.getMessage(), e);
         *     return false;
         * }
         */

        // Simulation avec 90% de succès - remplacer par vraie logique opérateur
        return Math.random() > 0.1;
    }

    /**
     * Crée un objet TransactionCallback pour le déclenchement du callback HTTP.
     *
     * @param transaction La transaction pour laquelle créer le callback
     * @return Le callback configuré avec payload et headers
     */
    private TransactionCallback createTransactionCallback(Transaction transaction) {
        return TransactionCallback.createNew(
                transaction.getTransactionId(),
                transaction.getCallbackUrl(),
                buildCallbackPayload(transaction),
                buildCallbackHeaders(transaction)
        );
    }

    /**
     * Construit le payload JSON pour le callback HTTP.
     * Sérialise les informations essentielles de la transaction.
     *
     * @param transaction La transaction source
     * @return Le payload JSON sous forme de chaîne
     */
    private String buildCallbackPayload(Transaction transaction) {
        // En production, utiliser ObjectMapper de Jackson pour la sérialisation JSON
        return """
                {
                    "transactionId": "%s",
                    "correlationId": "%s",
                    "status": "%s",
                    "montant": %s,
                    "telephone": "%s",
                    "timestamp": "%s"
                }
                """.formatted(
                transaction.getTransactionId(),
                transaction.getCorrelationId(),
                transaction.getStatus(),
                transaction.getMontant(),
                transaction.getTelephone(),
                transaction.getUpdatedAt()
        );
    }

    /**
     * Construit les headers HTTP personnalisés pour le callback.
     * Peut inclure des headers d'authentification ou de traçabilité.
     *
     * @param transaction La transaction source
     * @return Les headers sous forme de chaîne (null si aucun header spécifique)
     */
    private String buildCallbackHeaders(Transaction transaction) {
        // Exemple d'headers personnalisés pour le callback
        return """
                {
                    "%s": "application/json",
                    "X-Transaction-Id": "%s",
                    "X-Correlation-Id": "%s"
                }
                """.formatted(
                HttpHeaders.CONTENT_TYPE,
                transaction.getTransactionId(),
                transaction.getCorrelationId()
        );
    }

    /**
     * Méthode de fallback pour le circuit breaker en cas d'échec répété des callbacks.
     * Implémente une logique de dégradation gracieuse.
     *
     * @param transaction La transaction concernée par le fallback
     * @param t L'exception qui a déclenché le fallback
     */
    public void callbackFallback(Transaction transaction, Throwable t) {
        log.warn("Callback fallback triggered for transactionId={}: {}",
                transaction.getTransactionId(), t.getMessage());

        // Logique alternative : alertes, stockage pour retry manuel, etc.
        handleCallbackFallback(transaction, t);
    }

    /**
     * Gère les scénarios de fallback pour les callbacks échoués.
     * Peut inclure l'envoi d'alertes ou le stockage pour traitement différé.
     *
     * @param transaction La transaction concernée
     * @param throwable L'exception qui a causé le fallback
     */
    private void handleCallbackFallback(Transaction transaction, Throwable throwable) {
        log.error("CRITICAL_CALLBACK_FALLBACK - transactionId: {}, callbackUrl: {}, error: {}",
                transaction.getTransactionId(),
                transaction.getCallbackUrl(),
                throwable.getMessage());

        // TODO: Implémenter selon les besoins
        // - Alerting système (email, Slack, etc.)
        // - Stockage en queue pour retry manuel
        // - Création de ticket de support automatique
    }
}