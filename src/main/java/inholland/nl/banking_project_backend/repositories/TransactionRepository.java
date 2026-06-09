package inholland.nl.banking_project_backend.repositories;

import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.models.TransactionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, Long> {
    @Query("""
            SELECT t FROM TransactionModel t
            LEFT JOIN t.fromAccount fa
            LEFT JOIN fa.customer fcp
            LEFT JOIN fcp.user fu
            LEFT JOIN t.toAccount ta
            LEFT JOIN ta.customer tcp
            LEFT JOIN tcp.user tu
            WHERE t.timestamp BETWEEN :start AND :end
              AND (:viewerEmail IS NULL OR fu.email = :viewerEmail OR tu.email = :viewerEmail)
              AND (:amountLessThan IS NULL OR t.amount < :amountLessThan)
              AND (:amountGreaterThan IS NULL OR t.amount > :amountGreaterThan)
              AND (:amountEqualTo IS NULL OR t.amount = :amountEqualTo)
              AND (:iban IS NULL OR t.fromIbanSnapshot = :iban OR t.toIbanSnapshot = :iban)
              AND (:customerUserId IS NULL OR fu.id = :customerUserId OR tu.id = :customerUserId)
            ORDER BY t.timestamp DESC
            """)
    Page<TransactionModel> findTransactions(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("viewerEmail") String viewerEmail,
            @Param("amountLessThan") BigDecimal amountLessThan,
            @Param("amountGreaterThan") BigDecimal amountGreaterThan,
            @Param("amountEqualTo") BigDecimal amountEqualTo,
            @Param("iban") String iban,
            @Param("customerUserId") Long customerUserId,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM TransactionModel t
            LEFT JOIN t.fromAccount fa
            LEFT JOIN fa.customer fcp
            LEFT JOIN fcp.user fu
            LEFT JOIN t.toAccount ta
            LEFT JOIN ta.customer tcp
            LEFT JOIN tcp.user tu
            WHERE t.id = :id
              AND (:viewerEmail IS NULL OR fu.email = :viewerEmail OR tu.email = :viewerEmail)
            """)
    Optional<TransactionModel> findTransactionById(
            @Param("id") Long id,
            @Param("viewerEmail") String viewerEmail
    );

    @Query("""
            SELECT SUM(t.amount)
            FROM TransactionModel t
            WHERE t.fromIbanSnapshot = :iban
              AND t.timestamp BETWEEN :start AND :end
              AND t.type IN :outgoingTypes
            """)
    BigDecimal sumOutgoingAmountForAccount(
            @Param("iban") String iban,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("outgoingTypes") List<TransactionTypeEnum> outgoingTypes
    );

    @Query("""
            SELECT SUM(t.amount)
            FROM TransactionModel t
            WHERE t.toIbanSnapshot = :iban
              AND t.timestamp BETWEEN :start AND :end
              AND t.type = :depositType
            """)
    BigDecimal sumDepositAmountForAccount(
            @Param("iban") String iban,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("depositType") TransactionTypeEnum depositType
    );
}
