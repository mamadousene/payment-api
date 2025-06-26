package sn.com.developer.paymentapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Accéder dans le code Java aux propriétés personnalisées payment.api et payment.callback
 * définies dans le fichier application.yml, de manière propre et typée.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private Api api = new Api();
    private Callback callback = new Callback();

    @Getter @Setter
    public static class Api {
        private String key;
    }

    @Getter @Setter
    public static class Callback {
        private int maxRetries;
        private String timeout;
    }
}
