package sn.com.developer.paymentapi.config;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.com.developer.paymentapi.domain.entity.Operateur;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;
import sn.com.developer.paymentapi.repository.OperateurRepository;

import java.math.BigDecimal;
import java.util.Set;


/**
 * Initialiser automatiquement en base de données les opérateurs définis dans l'enum OperateurEnum au démarrage de l'application, s’ils n'existent pas déjà.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final OperateurRepository operateurRepository;

    @Bean
    @Transactional
    public ApplicationRunner initializeOperateurs() {
        return args -> {
            // Pour chaque opérateur défini dans l'enum
            for (OperateurEnum enumOperateur : OperateurEnum.values()) {
                // Vérifie si l'opérateur existe déjà en base
                if (operateurRepository.existsByCode(enumOperateur)) {
                    log.info("Operateur {} déjà présent en base, on skip", enumOperateur.getCode());
                    continue;
                }

                // Sinon on crée et on sauvegarde
                Operateur operateur = Operateur.builder()
                        .code(enumOperateur)
                        .displayName(enumOperateur.getDisplayName())
                        .apiEndpoint("https://api." + enumOperateur.getCode().toLowerCase() + ".com")
                        .timeoutSeconds(30)
                        .maxRetry(3)
                        .isActive(true)
                        .commission(BigDecimal.ZERO)
                        .currency("XOF")
                        .prefixes(Set.of(enumOperateur.getPrefixes()))
                        .build();

                operateurRepository.save(operateur);
                log.info("Operateur {} inséré en base avec préfixes {}", enumOperateur.getCode(), operateur.getPrefixes());
            }
        };
    }
}
