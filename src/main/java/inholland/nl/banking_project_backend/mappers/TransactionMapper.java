package inholland.nl.banking_project_backend.mappers;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionResponseDTO;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransactionMapper {

    public TransactionModel toModel(
            CreateTransactionRequestDTO request,
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

    public TransactionResponseDTO toResponse(TransactionModel transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getFromIbanSnapshot(),
                transaction.getToIbanSnapshot(),
                signedAmount(transaction),
                transaction.getTimestamp(),
                transaction.getInitiatedBy().getEmail()
        );
    }

    private String getIban(AccountModel account) {
        return account == null ? null : account.getIban();
    }

    private BigDecimal signedAmount(TransactionModel transaction) {
        if (transaction.getType() == TransactionTypeEnum.WITHDRAWAL) {
            return transaction.getAmount().negate();
        }
        return transaction.getAmount();
    }
}
