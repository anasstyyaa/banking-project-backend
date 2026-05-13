package inholland.nl.banking_project_backend.mapper;

import inholland.nl.banking_project_backend.dtos.TransactionDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransactionServiceMapper {
    // Creates a transaction entity with stable IBAN snapshots for history.
    public TransactionModel toModel(
            TransactionDTO.CreateRequest request,
            AccountModel fromAccount,
            AccountModel toAccount,
            UserModel initiatedBy
    ) {
        TransactionModel transaction = new TransactionModel();
        transaction.setType(request.type());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setFromIbanSnapshot(getIban(fromAccount));
        transaction.setToIbanSnapshot(getIban(toAccount));
        transaction.setAmount(request.amount());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedBy(initiatedBy);
        return transaction;
    }

    // Converts a transaction entity into a frontend-safe transaction response.
    public TransactionDTO.TransactionResponse toResponse(TransactionModel transaction) {
        return new TransactionDTO.TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getFromIbanSnapshot(),
                transaction.getToIbanSnapshot(),
                signedAmount(transaction),
                transaction.getTimestamp(),
                transaction.getInitiatedBy().getEmail()
        );
    }

    // Reads the IBAN from an account when the account exists for this transaction type.
    private String getIban(AccountModel account) {
        return account == null ? null : account.getIban();
    }

    // Presents withdrawals as negative amounts while keeping stored amount positive.
    private BigDecimal signedAmount(TransactionModel transaction) {
        return transaction.getType().name().equals("WITHDRAWAL")
                ? transaction.getAmount().negate()
                : transaction.getAmount();
    }
}
