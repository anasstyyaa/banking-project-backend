package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.TransactionTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, Long> {
    @Query("""
            select coalesce(sum(t.amount), 0)
            from TransactionModel t
            where t.fromAccount = :account
            and t.type = :type
            and t.timestamp >= :start
            and t.timestamp < :end
            """)
    BigDecimal sumAmountByAccountTypeAndPeriod(
            @Param("account") AccountModel account,
            @Param("type") TransactionTypeEnum type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
