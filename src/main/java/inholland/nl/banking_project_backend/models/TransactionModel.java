package inholland.nl.banking_project_backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne
    private AccountModel fromAccount;

    @ManyToOne
    private AccountModel toAccount;

    @ManyToOne(optional = false)
    private UserModel initiatedBy;
}
