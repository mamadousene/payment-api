package sn.com.developer.paymentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.com.developer.paymentapi.domain.entity.Transaction;


/**
 * Repository JPA pour l'entité {@link Transaction}.
 * Fournit les opérations de base (CRUD) sur les transactions via {@link JpaRepository}.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}