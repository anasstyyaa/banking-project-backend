package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("""
        SELECT u FROM UserModel u
        WHERE u.isApproved = :isApproved
          AND (:term IS NULL OR :term = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.bsn) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.iban) LIKE LOWER(CONCAT('%', :term, '%')))
        """)
    Page<UserModel> findByApprovalStatus(
            @Param("isApproved") boolean isApproved,
            @Param("term") String term,
            Pageable pageable
    );
}
