package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    // Finds a user by the email used for authentication.
    Optional<UserModel> findByEmail(String email);

    // Checks whether a user email is already registered.
    boolean existsByEmail(String email);
}
