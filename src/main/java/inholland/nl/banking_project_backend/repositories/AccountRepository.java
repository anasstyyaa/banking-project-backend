package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Long> {
    List<AccountModel> findByUserEmail(String email);

    Optional<AccountModel> findByIdAndUserEmail(Long id, String email);
}
