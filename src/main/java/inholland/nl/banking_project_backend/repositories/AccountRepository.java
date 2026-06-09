package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.AccountModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Long> {
    @Query("""
            SELECT a FROM AccountModel a
            WHERE a.isActive = true
              AND (:ownerEmail IS NULL OR a.customer.user.email = :ownerEmail)
            """)
    Page<AccountModel> findAccounts(@Param("ownerEmail") String ownerEmail, @Param("search") String search, Pageable pageable);
    @Query("""
            SELECT a FROM AccountModel a
            WHERE a.iban = :iban
              AND (:ownerEmail IS NULL OR a.customer.user.email = :ownerEmail)
            """)
    Optional<AccountModel> findAccountByIban(
            @Param("iban") String iban,
            @Param("ownerEmail") String ownerEmail
    );

    @Query("SELECT a FROM AccountModel a WHERE " +
            "a.isActive = true AND (" +
            "LOWER(a.customer.user.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(a.customer.user.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(a.iban) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<AccountModel> searchAccounts(@Param("term") String term, Pageable pageable);
}
