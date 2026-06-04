package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Long> {
    // Finds an account by its unique IBAN.
    Optional<AccountModel> findByIban(String iban);

    // Finds all active accounts owned by the customer with this user email.
    List<AccountModel> findByCustomerUserEmailAndIsActiveTrue(String email);

    // Finds one account owned by the customer with this user email.
    Optional<AccountModel> findByIbanAndCustomerUserEmail(String iban, String email);

    // Finds all active accounts for employee account management.
    List<AccountModel> findByIsActiveTrue();

    // Searches active accounts by customer name or IBAN.
    @Query("SELECT a FROM AccountModel a WHERE " +
            "a.isActive = true AND (" +
            "LOWER(a.customer.user.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(a.customer.user.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(a.iban) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<AccountModel> searchActiveAccounts(@Param("term") String term);
}
