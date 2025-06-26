package sn.com.developer.paymentapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import sn.com.developer.paymentapi.config.PaymentProperties;
import sn.com.developer.paymentapi.domain.entity.Operateur;
import sn.com.developer.paymentapi.domain.entity.Transaction;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;
import sn.com.developer.paymentapi.domain.enums.TransactionStatusEnum;
import sn.com.developer.paymentapi.exception.BadRequestAlertException;
import sn.com.developer.paymentapi.repository.OperateurRepository;
import sn.com.developer.paymentapi.repository.TransactionRepository;
import sn.com.developer.paymentapi.service.Impl.PaymentServiceImpl;
import sn.com.developer.paymentapi.service.dto.PaymentRequest;
import sn.com.developer.paymentapi.service.dto.PaymentResponse;
import sn.com.developer.paymentapi.utils.TransactionIdGenerator;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock private OperateurRepository operateurRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionIdGenerator transactionIdGenerator;
    @Mock private PaymentProperties paymentProperties;
    @Mock private AsyncTaskExecutor taskExecutor;
    @Mock private PaymentProperties.Api apiConfig;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_Success() {
        // Given
        when(paymentProperties.getApi()).thenReturn(apiConfig);
        when(apiConfig.getKey()).thenReturn("valid-key");
        when(operateurRepository.findByCode(OperateurEnum.ORANGE)).thenReturn(Optional.of(createOperateur()));
        when(transactionIdGenerator.generateWithPrefix("ORANGE")).thenReturn("ORANGE-123");
        when(transactionRepository.save(any(Transaction.class))).thenReturn(createTransaction());

        PaymentRequest request = new PaymentRequest(
                new BigDecimal("1000"), "771234567", "corr-123",
                OperateurEnum.ORANGE, "http://callback.com"
        );

        // When
        PaymentResponse response = paymentService.processPayment(request, "valid-key");

        // Then
        assertThat(response.transactionId()).isEqualTo("ORANGE-123");
        assertThat(response.status()).isEqualTo(TransactionStatusEnum.PENDING);
        assertThat(response.message()).isEqualTo("Transaction initiée avec succès");

        verify(transactionRepository).save(any(Transaction.class));
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void processPayment_InvalidApiKey() {
        // Given
        when(paymentProperties.getApi()).thenReturn(apiConfig);
        when(apiConfig.getKey()).thenReturn("valid-key");

        PaymentRequest request = new PaymentRequest(
                new BigDecimal("1000"), "771234567", "corr-123",
                OperateurEnum.ORANGE, null
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request, "invalid-key"))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("Clé API invalide");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processPayment_UnsupportedOperator() {
        // Given
        when(paymentProperties.getApi()).thenReturn(apiConfig);
        when(apiConfig.getKey()).thenReturn("valid-key");
        when(operateurRepository.findByCode(OperateurEnum.YAS)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest(
                new BigDecimal("1000"), "751234567", "corr-123",
                OperateurEnum.YAS, null
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request, "valid-key"))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("Opérateur non supporté");
    }

    private Operateur createOperateur() {
        return Operateur.builder()
                .code(OperateurEnum.ORANGE)
                .displayName("Orange Money")
                .build();
    }

    private Transaction createTransaction() {
        return Transaction.builder()
                .transactionId("ORANGE-123")
                .correlationId("corr-123")
                .montant(new BigDecimal("1000"))
                .telephone("771234567")
                .status(TransactionStatusEnum.PENDING)
                .build();
    }
}