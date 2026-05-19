package inholland.nl.banking_project_backend.models;

import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class TransactionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionTypeEnum type;

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AccountModel fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AccountModel toAccount;

    private String fromIbanSnapshot;
    private String toIbanSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserModel initiatedBy;
}
