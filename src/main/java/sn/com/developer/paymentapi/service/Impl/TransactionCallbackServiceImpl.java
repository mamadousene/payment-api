package sn.com.developer.paymentapi.service.Impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import sn.com.developer.paymentapi.config.PaymentProperties;
import sn.com.developer.paymentapi.domain.entity.TransactionCallback;
import sn.com.developer.paymentapi.domain.enums.CallbackStatusEnum;
import sn.com.developer.paymentapi.repository.TransactionCallbackRepository;
import sn.com.developer.paymentapi.service.TransactionCallbackService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * TransactionCallbackServiceImpl gère le traitement, l’envoi,
 * le suivi et les éventuelles relances (retries) des callbacks vers les systèmes clients après une transaction de paiement.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionCallbackServiceImpl implements TransactionCallbackService {

    private final TransactionCallbackRepository transactionCallbackRepository;
    private final RestTemplate restTemplate;
    private final PaymentProperties paymentProperties;

    @Qualifier("payment-async-task-executor")
    private final AsyncTaskExecutor taskExecutor;

    /**
     * Déclenche l'envoi asynchrone d'un callback de transaction avec gestion des retry et circuit breaker.
     * Vérifie d'abord si le nombre maximum de tentatives n'est pas atteint avant d'exécuter le callback.
     *
     * @param callback L'objet TransactionCallback contenant les informations du callback à envoyer
     */
    @Async("payment-async-task-executor")
    @Transactional
    @Retry(name = "callbackService")
    @CircuitBreaker(name = "callbackService", fallbackMethod = "callbackFallback")
    public void triggerCallbackAsync(TransactionCallback callback) {
        if (shouldSkipCallback(callback)) {
            return;
        }

        prepareCallbackForExecution(callback);

        executeCallback(callback)
                .ifPresentOrElse(
                        response -> handleSuccessfulCallback(callback, response),
                        () -> scheduleRetryIfPossible(callback)
                );
    }

    /**
     * Méthode de fallback appelée par le Circuit Breaker en cas d'échec répété.
     * Gère les scénarios de dégradation gracieuse du service.
     *
     * @param callback Le callback qui a déclenché le fallback
     * @param t L'exception ou erreur qui a provoqué le fallback
     */
    public void callbackFallback(TransactionCallback callback, Throwable t) {
        log.error("Callback fallback triggered for transactionId={}: {}",
                callback.getTransactionId(), t.getMessage());
        handleFallbackScenario(callback, t);
    }

    /**
     * Vérifie si le callback doit être ignoré en fonction du nombre maximum de tentatives autorisées.
     * Si le maximum est atteint, marque le callback comme épuisé.
     *
     * @param callback Le callback de transaction à vérifier
     * @return true si le callback doit être ignoré, false sinon
     */
    private boolean shouldSkipCallback(TransactionCallback callback) {
        int maxRetries = paymentProperties.getCallback().getMaxRetries();

        if (callback.getAttemptCount() >= maxRetries) {
            log.warn("Callback max retries reached for transactionId={}, attempts={}",
                    callback.getTransactionId(), callback.getAttemptCount());
            markCallbackAsExhausted(callback);
            return true;
        }

        return false;
    }

    /**
     * Prépare le callback pour l'exécution en mettant à jour son statut et en incrémentant le compteur de tentatives.
     * Enregistre les modifications en base de données et log l'événement.
     *
     * @param callback Le callback à préparer pour l'exécution
     */
    private void prepareCallbackForExecution(TransactionCallback callback) {
        log.info("Sending callback transactionId={}, attempt={}",
                callback.getTransactionId(), callback.getAttemptCount() + 1);

        callback.markAsInProgress();
        callback.incrementAttemptCount();
        transactionCallbackRepository.save(callback);
    }

    /**
     * Exécute le callback HTTP et traite la réponse.
     * Retourne un Optional contenant la réponse en cas de succès, vide en cas d'échec.
     *
     * @param callback Le callback de transaction à exécuter
     * @return Optional contenant la réponse du callback si succès, Optional.empty() si échec
     */
    private Optional<CallbackResponse> executeCallback(TransactionCallback callback) {
        return tryExecuteHttpCallback(callback)
                .filter(this::isSuccessfulResponse)
                .or(() -> {
                    log.warn("Callback failed for transactionId={}", callback.getTransactionId());
                    return Optional.empty();
                });
    }

    /**
     * Tente d'exécuter l'appel HTTP du callback avec gestion des exceptions.
     * Encapsule la logique d'appel REST et la gestion d'erreur.
     *
     * @param callback Le callback contenant l'URL et les données à envoyer
     * @return Optional contenant la réponse HTTP ou vide en cas d'exception
     */
    private Optional<CallbackResponse> tryExecuteHttpCallback(TransactionCallback callback) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    callback.getCallbackUrl(),
                    callback.getRequestPayload(),
                    String.class
            );

            return Optional.of(new CallbackResponse(
                    response.getStatusCode().value(),
                    response.getBody()
            ));

        } catch (Exception e) {
            log.error("HTTP callback exception for transactionId={}: {}",
                    callback.getTransactionId(), e.getMessage());
            handleCallbackException(callback, e);
            return Optional.empty();
        }
    }

    /**
     * Détermine si la réponse HTTP est considérée comme un succès.
     * Une réponse est considérée comme réussie si le code de statut est entre 200 et 299.
     *
     * @param response La réponse du callback à évaluer
     * @return true si la réponse indique un succès, false sinon
     */
    private boolean isSuccessfulResponse(CallbackResponse response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Traite un callback réussi en mettant à jour le statut en base et en loggant l'événement.
     *
     * @param callback Le callback de transaction qui a réussi
     * @param response La réponse HTTP reçue du callback
     */
    private void handleSuccessfulCallback(TransactionCallback callback, CallbackResponse response) {
        callback.markAsSuccess(response.statusCode(), response.body(), null, null);
        transactionCallbackRepository.save(callback);
        log.info("Callback SUCCESS for transactionId={}", callback.getTransactionId());
    }

    /**
     * Gère les exceptions survenues lors de l'exécution du callback.
     * Met à jour le statut du callback en échec et sauvegarde en base.
     *
     * @param callback Le callback qui a échoué
     * @param e L'exception qui s'est produite
     */
    private void handleCallbackException(TransactionCallback callback, Exception e) {
        callback.markAsFailed(e.getMessage(), null, null, null);
        transactionCallbackRepository.save(callback);
    }

    /**
     * Planifie un nouveau retry si possible, sinon marque le callback comme épuisé.
     * Vérifie la capacité de retry avant de programmer une nouvelle tentative.
     *
     * @param callback Le callback pour lequel planifier un retry
     */
    private void scheduleRetryIfPossible(TransactionCallback callback) {
        if (callback.canRetry()) {
            scheduleNextRetry(callback);
        } else {
            markCallbackAsExhausted(callback);
        }
    }

    /**
     * Planifie la prochaine tentative de callback avec un délai calculé.
     * Utilise CompletableFuture pour programmer l'exécution asynchrone différée.
     *
     * @param callback Le callback à programmer pour une nouvelle tentative
     */
    private void scheduleNextRetry(TransactionCallback callback) {
        callback.scheduleNextRetry();
        transactionCallbackRepository.save(callback);

        CompletableFuture
                .delayedExecutor(calculateRetryDelay(callback), java.util.concurrent.TimeUnit.MILLISECONDS, taskExecutor)
                .execute(() -> triggerCallbackAsync(callback));
    }

    /**
     * Calcule le délai en millisecondes avant la prochaine tentative de callback.
     *
     * @param callback Le callback contenant la date de prochaine tentative
     * @return Le délai en millisecondes avant la prochaine tentative
     */
    private long calculateRetryDelay(TransactionCallback callback) {
        return Duration.between(LocalDateTime.now(), callback.getNextRetryAt()).toMillis();
    }

    /**
     * Marque le callback comme épuisé lorsque le nombre maximum de tentatives est atteint.
     * Met à jour le statut en base et log l'événement.
     *
     * @param callback Le callback à marquer comme épuisé
     */
    private void markCallbackAsExhausted(TransactionCallback callback) {
        log.warn("Callback exhausted for transactionId={}", callback.getTransactionId());
        callback.setCallbackStatus(CallbackStatusEnum.EXHAUSTED);
        transactionCallbackRepository.save(callback);
    }



    /**
     * Gère les scénarios de fallback en implémentant la logique de dégradation.
     * Peut inclure l'envoi d'alertes, l'écriture dans une queue de lettres mortes, etc.
     *
     * @param callback Le callback concerné par le fallback
     * @param t L'exception qui a déclenché le fallback
     */
    private void handleFallbackScenario(TransactionCallback callback, Throwable t) {
        // Marquer le callback comme définitivement échoué
        callback.setCallbackStatus(CallbackStatusEnum.EXHAUSTED);
        transactionCallbackRepository.save(callback);

        // Log détaillé pour monitoring et debug
        log.error("Fallback scenario activated - transactionId={}, attempts={}, lastError={}",
                callback.getTransactionId(),
                callback.getAttemptCount(),
                t.getMessage());

        // TODO: Implémenter selon les besoins business
        // - Envoyer une alerte email/Slack aux ops
        // - Écrire dans une dead letter queue pour traitement manuel
        // - Déclencher un webhook d'alerte système
        // - Créer un ticket automatique dans le système de ticketing

        // Exemple d'implémentation basique d'alerte
        notifyOperationsTeam(callback, t);
    }

    /**
     * Notifie l'équipe opérationnelle d'un échec critique de callback.
     * Implémentation basique qui peut être étendue selon les besoins.
     *
     * @param callback Le callback en échec
     * @param t L'exception qui a causé l'échec
     */
    private void notifyOperationsTeam(TransactionCallback callback, Throwable t) {
        // Log structuré pour les outils de monitoring (ELK, Datadog, etc.)
        log.error("CRITICAL_CALLBACK_FAILURE - transactionId: {}, callbackUrl: {}, errorType: {}",
                callback.getTransactionId(),
                callback.getCallbackUrl(),
                t.getClass().getSimpleName());

        // TODO: Intégrer avec votre système d'alerting
        // - Email via service de notification
        // - Webhook Slack/Teams
        // - Push notification mobile pour l'équipe d'astreinte
    }

    /**
     * Record encapsulant la réponse d'un callback HTTP.
     * Contient le code de statut et le corps de la réponse pour un traitement simplifié.
     *
     * @param statusCode Le code de statut HTTP de la réponse
     * @param body Le corps de la réponse HTTP
     */
    private record CallbackResponse(int statusCode, String body) {}
}