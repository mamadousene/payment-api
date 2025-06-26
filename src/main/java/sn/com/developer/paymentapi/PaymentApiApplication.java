package sn.com.developer.paymentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Application principale pour l'API de paiement mobile
 * Supporte les opérateurs ORANGE, EXPRESSO, YAS au Sénégal
 *
 * @author mlsene
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class PaymentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApiApplication.class, args);
    }

}
