package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByUsername(String username);
    Optional<UserModel> findByEmail(String email);
    boolean existsByEmail(String email);
    List<UserModel> findAllByIsApprovedFalse();
    List<UserModel> findAllByIsApprovedTrue();
}
