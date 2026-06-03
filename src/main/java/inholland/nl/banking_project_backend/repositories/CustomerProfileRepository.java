package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileModel, Long> {
    Optional<CustomerProfileModel> findByUserEmail(String email);
}
