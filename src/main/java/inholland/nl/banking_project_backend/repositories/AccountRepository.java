package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Long> {
    // Finds an account by its unique IBAN.
    Optional<AccountModel> findByIban(String iban);

    // Finds all accounts owned by the customer with this user email.
    List<AccountModel> findByCustomerUserEmail(String email);

    // find customer iban by name
    List<AccountModel> findByCustomerUserFirstNameContainingIgnoreCaseOrCustomerUserLastNameContainingIgnoreCase(String firstName, String lastName);

}
