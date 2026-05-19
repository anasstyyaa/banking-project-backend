package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, Long> {
    // Finds transactions inside a date-time range newest first.
    List<TransactionModel> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    // Finds outgoing transactions for one source IBAN inside a date-time range.
    List<TransactionModel> findByFromIbanSnapshotAndTimestampBetween(String iban, LocalDateTime start, LocalDateTime end);
}
