package sn.com.developer.paymentapi.service;


import sn.com.developer.paymentapi.service.dto.PaymentRequest;
import sn.com.developer.paymentapi.service.dto.PaymentResponse;

/**
 * Service principal pour le traitement des paiements
 */

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request, String apiKey);
}