package sn.com.developer.paymentapi.domain.enums;

/**
 * Définir les différents états possibles d'un callback dans le système de paiement, tels que le succès, l’échec,
 * l’épuisement des tentatives, ou les différents états d’attente ou de progression.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */

public enum CallbackStatusEnum {
    SUCCESS,FAILED,EXHAUSTED,IN_PROGRESS,PENDING_RETRY,PENDING
}
