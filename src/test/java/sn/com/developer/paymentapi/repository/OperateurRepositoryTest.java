package sn.com.developer.paymentapi.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import sn.com.developer.paymentapi.domain.entity.Operateur;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:jpa-test-application.yml")
class OperateurRepositoryTest {

    @Autowired
    private OperateurRepository operateurRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @DisplayName("Doit trouver un opérateur par son code")
    void findByCode_WhenOperateurExists_ShouldReturnOperateur() {
        // Given
        Operateur operateur = createOperateur(OperateurEnum.ORANGE);
        testEntityManager.persistAndFlush(operateur);

        // When
        Optional<Operateur> result = operateurRepository.findByCode(OperateurEnum.ORANGE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo(OperateurEnum.ORANGE);
        assertThat(result.get().getDisplayName()).isEqualTo("Orange Money");
        assertThat(result.get().getPrefixes()).containsExactlyInAnyOrder("77", "78");
    }

    @Test
    @DisplayName("Doit retourner Optional.empty() quand l'opérateur n'existe pas")
    void findByCode_WhenOperateurDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Operateur> result = operateurRepository.findByCode(OperateurEnum.ORANGE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Doit retourner true quand l'opérateur existe")
    void existsByCode_WhenOperateurExists_ShouldReturnTrue() {
        // Given
        Operateur operateur = createOperateur(OperateurEnum.EXPRESSO);
        testEntityManager.persistAndFlush(operateur);

        // When
        boolean exists = operateurRepository.existsByCode(OperateurEnum.EXPRESSO);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Doit retourner false quand l'opérateur n'existe pas")
    void existsByCode_WhenOperateurDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = operateurRepository.existsByCode(OperateurEnum.YAS);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Doit gérer plusieurs opérateurs avec des codes différents")
    void findByCode_WithMultipleOperateurs_ShouldReturnCorrectOne() {
        // Given
        Operateur orange = createOperateur(OperateurEnum.ORANGE);
        Operateur expresso = createOperateur(OperateurEnum.EXPRESSO);
        Operateur yas = createOperateur(OperateurEnum.YAS);

        testEntityManager.persist(orange);
        testEntityManager.persist(expresso);
        testEntityManager.persist(yas);
        testEntityManager.flush();

        // When
        Optional<Operateur> orangeResult = operateurRepository.findByCode(OperateurEnum.ORANGE);
        Optional<Operateur> expressoResult = operateurRepository.findByCode(OperateurEnum.EXPRESSO);
        Optional<Operateur> yasResult = operateurRepository.findByCode(OperateurEnum.YAS);

        // Then
        assertThat(orangeResult).isPresent();
        assertThat(orangeResult.get().getCode()).isEqualTo(OperateurEnum.ORANGE);

        assertThat(expressoResult).isPresent();
        assertThat(expressoResult.get().getCode()).isEqualTo(OperateurEnum.EXPRESSO);

        assertThat(yasResult).isPresent();
        assertThat(yasResult.get().getCode()).isEqualTo(OperateurEnum.YAS);
    }

    @Test
    @DisplayName("Doit vérifier l'existence de tous les opérateurs")
    void existsByCode_WithMultipleOperateurs_ShouldWorkCorrectly() {
        // Given
        Operateur orange = createOperateur(OperateurEnum.ORANGE);
        Operateur expresso = createOperateur(OperateurEnum.EXPRESSO);

        testEntityManager.persist(orange);
        testEntityManager.persist(expresso);
        testEntityManager.flush();

        // When & Then
        assertThat(operateurRepository.existsByCode(OperateurEnum.ORANGE)).isTrue();
        assertThat(operateurRepository.existsByCode(OperateurEnum.EXPRESSO)).isTrue();
        assertThat(operateurRepository.existsByCode(OperateurEnum.YAS)).isFalse();
    }

    @Test
    @DisplayName("Doit sauvegarder et récupérer un opérateur avec tous ses attributs")
    void save_ShouldPersistAllAttributes() {
        // Given
        Operateur operateur = Operateur.builder()
                .code(OperateurEnum.ORANGE)
                .displayName("Orange Money")
                .apiEndpoint("https://api.orange.com")
                .timeoutSeconds(45)
                .maxRetry(5)
                .isActive(false)
                .commission(new BigDecimal("2.50"))
                .currency("USD")
                .prefixes(Set.of("77", "78", "79"))
                .build();

        // When
        Operateur saved = operateurRepository.save(operateur);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Operateur> retrieved = operateurRepository.findByCode(OperateurEnum.ORANGE);

        // Then
        assertThat(retrieved).isPresent();
        Operateur retrievedOperateur = retrieved.get();

        assertThat(retrievedOperateur.getCode()).isEqualTo(OperateurEnum.ORANGE);
        assertThat(retrievedOperateur.getDisplayName()).isEqualTo("Orange Money");
        assertThat(retrievedOperateur.getApiEndpoint()).isEqualTo("https://api.orange.com");
        assertThat(retrievedOperateur.getTimeoutSeconds()).isEqualTo(45);
        assertThat(retrievedOperateur.getMaxRetry()).isEqualTo(5);
        assertThat(retrievedOperateur.getIsActive()).isFalse();
        assertThat(retrievedOperateur.getCommission()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(retrievedOperateur.getCurrency()).isEqualTo("USD");
        assertThat(retrievedOperateur.getPrefixes()).containsExactlyInAnyOrder("77", "78", "79");
    }

    @Test
    @DisplayName("Doit gérer les préfixes correctement pour chaque opérateur")
    void findByCode_ShouldReturnCorrectPrefixes() {
        // Given
        Operateur orange = createOperateur(OperateurEnum.ORANGE);
        Operateur expresso = createOperateur(OperateurEnum.EXPRESSO);
        Operateur yas = createOperateur(OperateurEnum.YAS);

        testEntityManager.persist(orange);
        testEntityManager.persist(expresso);
        testEntityManager.persist(yas);
        testEntityManager.flush();

        // When & Then
        Optional<Operateur> orangeResult = operateurRepository.findByCode(OperateurEnum.ORANGE);
        assertThat(orangeResult).isPresent();
        assertThat(orangeResult.get().getPrefixes()).containsExactlyInAnyOrder("77", "78");

        Optional<Operateur> expressoResult = operateurRepository.findByCode(OperateurEnum.EXPRESSO);
        assertThat(expressoResult).isPresent();
        assertThat(expressoResult.get().getPrefixes()).containsExactly("70");

        Optional<Operateur> yasResult = operateurRepository.findByCode(OperateurEnum.YAS);
        assertThat(yasResult).isPresent();
        assertThat(yasResult.get().getPrefixes()).containsExactlyInAnyOrder("76", "75");
    }

    /**
     * Méthode utilitaire pour créer un opérateur de test basé sur l'enum
     */
    private Operateur createOperateur(OperateurEnum enumOperateur) {
        return Operateur.builder()
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
    }
}