package sn.com.developer.paymentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.com.developer.paymentapi.domain.entity.Operateur;
import sn.com.developer.paymentapi.domain.enums.OperateurEnum;

import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Operateur}.
 * Fournit les opérations CRUD de base ainsi que des méthodes personnalisées
 * pour accéder aux opérateurs via leur code.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 *
 */

public interface OperateurRepository extends JpaRepository<Operateur, Long> {

    /**
     * Recherche un opérateur par son code.
     *
     * @param code Le code de l'opérateur (valeur de l'énumération {@link OperateurEnum}).
     * @return Un {@link Optional} contenant l'opérateur si trouvé, sinon vide.
     */
    Optional<Operateur> findByCode(OperateurEnum code);

    /**
     * Vérifie si un opérateur existe avec le code donné.
     *
     * @param code Le code de l'opérateur à vérifier.
     * @return true si un opérateur avec ce code existe, false sinon.
     */
    boolean existsByCode(OperateurEnum code);
}
