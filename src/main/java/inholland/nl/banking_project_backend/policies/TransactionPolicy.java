package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.LimitExceededException;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionPolicy {
    public void validateCustomerTransfer(CreateTransactionRequestDTO request, AccountModel source, AccountModel destination, BigDecimal dailyOutgoingTotal) {
        requireTransactionType(request, TransactionTypeEnum.TRANSFER);
        requireAccountField(source, "Transfer requires a source IBAN.");
        requireAccountField(destination, "Transfer requires a destination IBAN.");
        requireOpenAccount(source);
        requireOpenAccount(destination);
        requireDifferentAccounts(source, destination);
        requireExternalCustomerTransferAccounts(source, destination);
        requireOutgoingLimits(source, request.amount(), dailyOutgoingTotal);
    }

    public void validateEmployeeTransfer(CreateTransactionRequestDTO request, AccountModel source, AccountModel destination, BigDecimal dailyOutgoingTotal) {
        requireTransactionType(request, TransactionTypeEnum.TRANSFER);
        requireAccountField(source, "Transfer requires a source IBAN.");
        requireAccountField(destination, "Transfer requires a destination IBAN.");
        requireOpenAccount(source);
        requireOpenAccount(destination);
        requireCheckingTransfer(source, destination, "Employees can only transfer between checking accounts.");
        requireDifferentAccounts(source, destination);
        requireOutgoingLimits(source, request.amount(), dailyOutgoingTotal);
    }

    public void validateDeposit(CreateTransactionRequestDTO request, AccountModel destination, BigDecimal dailyDepositTotal) {
        requireTransactionType(request, TransactionTypeEnum.DEPOSIT);
        requireAccountField(destination, "Deposit requires a destination IBAN.");
        requireOpenAccount(destination);
        requireCheckingAccount(destination, "ATM deposits can only be made into checking accounts.");

        if (dailyDepositTotal.add(request.amount()).compareTo(destination.getDailyLimit()) > 0) {
            throw new LimitExceededException("This transaction exceeds the account daily limit.");
        }
    }

    public void validateWithdrawal(CreateTransactionRequestDTO request, AccountModel source, BigDecimal dailyOutgoingTotal) {
        requireTransactionType(request, TransactionTypeEnum.WITHDRAWAL);
        requireAccountField(source, "Withdrawal requires a source IBAN.");
        requireOpenAccount(source);
        requireCheckingAccount(source, "ATM withdrawals can only be made from checking accounts.");
        requireOutgoingLimits(source, request.amount(), dailyOutgoingTotal);
    }

    private void requireTransactionType(CreateTransactionRequestDTO request, TransactionTypeEnum expectedType) {
        if (request.type() != expectedType) {
            throw new IllegalArgumentException("Transaction type does not match this operation.");
        }
    }

    private void requireAccountField(AccountModel account, String message) {
        if (account == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireOpenAccount(AccountModel account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalStateException("This account is closed.");
        }
    }

    private void requireDifferentAccounts(AccountModel source, AccountModel destination) {
        if (source.getIban().equals(destination.getIban())) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
    }

    private void requireExternalCustomerTransferAccounts(AccountModel source, AccountModel destination) {
        if (!accountsBelongToSameCustomer(source, destination)) {
            requireCheckingTransfer(source, destination, "External customer transfers can only be made between checking accounts.");
        }
    }

    private void requireCheckingTransfer(AccountModel source, AccountModel destination, String message) {
        if (source.getType() != AccountTypeEnum.CHECKING || destination.getType() != AccountTypeEnum.CHECKING) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean accountsBelongToSameCustomer(AccountModel source, AccountModel destination) {
        CustomerProfileModel sourceCustomer = source.getCustomer();
        CustomerProfileModel destinationCustomer = destination.getCustomer();

        if (sourceCustomer == null || destinationCustomer == null) {
            return false;
        }

        if (sourceCustomer.getId() != null && destinationCustomer.getId() != null) {
            return sourceCustomer.getId().equals(destinationCustomer.getId());
        }

        return sourceCustomer == destinationCustomer;
    }

    private void requireCheckingAccount(AccountModel account, String message) {
        if (account.getType() != AccountTypeEnum.CHECKING) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireOutgoingLimits(AccountModel source, BigDecimal amount, BigDecimal dailyOutgoingTotal) {
        BigDecimal balanceAfterTransaction = source.getBalance().subtract(amount);
        if (balanceAfterTransaction.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new LimitExceededException("This transaction exceeds the account absolute limit.");
        }

        if (dailyOutgoingTotal.add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new LimitExceededException("This transaction exceeds the account daily limit.");
        }
    }
}
