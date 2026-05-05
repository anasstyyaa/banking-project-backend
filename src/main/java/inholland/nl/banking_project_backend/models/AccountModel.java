package inholland.nl.banking_project_backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
public class AccountModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountTypeEnum type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal absoluteLimit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    private UserModel user;
}
