package sn.com.developer.paymentapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;
import sn.com.developer.paymentapi.service.dto.PaymentRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PaymentControllerIT {

    private static final String PAYMENT_API_URL = "/api/v1/payment-api/envoi";
    private static final String API_KEY_HEADER = "API-KEY";
    private static final String VALID_API_KEY = "SN_KEY_TEST_44785dfrt79SKFKF578"; // à adapter selon ta logique

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldReturn200_whenValidPaymentRequest() throws Exception {
        // Préparer une requête de paiement valide
        PaymentRequest request = new PaymentRequest(
                BigDecimal.valueOf(2000),
                "771781104",
                UUID.randomUUID().toString(),
                OperateurEnum.ORANGE,
                "https://webhook.site/test-callback"
        );

        mockMvc.perform(post(PAYMENT_API_URL)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("PENDING")); // ou autre statut attendu
    }

    @Test
    public void shouldReturn401_whenApiKeyMissing() throws Exception {
        PaymentRequest request = new PaymentRequest(
                BigDecimal.valueOf(2000),
                "771781104",
                UUID.randomUUID().toString(),
                OperateurEnum.ORANGE,
                "https://webhook.site/test-callback"
        );

        mockMvc.perform(post(PAYMENT_API_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldReturn400_whenInvalidPayload() throws Exception {
        // Payload invalide : montant null
        PaymentRequest request = new PaymentRequest(
                null,
                "770000000",
                UUID.randomUUID().toString(),
                OperateurEnum.ORANGE,
                "https://webhook.site/test-callback"
                );

        mockMvc.perform(post(PAYMENT_API_URL)
                        .header(API_KEY_HEADER, VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
