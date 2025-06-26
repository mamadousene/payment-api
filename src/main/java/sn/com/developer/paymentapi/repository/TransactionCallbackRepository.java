package sn.com.developer.paymentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.com.developer.paymentapi.domain.entity.TransactionCallback;

/**
 * Repository JPA pour l'entité {@link TransactionCallback}.
 * Fournit les opérations CRUD de base pour gérer les callbacks de transaction.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */

public interface TransactionCallbackRepository extends JpaRepository<TransactionCallback, Long> {
}
