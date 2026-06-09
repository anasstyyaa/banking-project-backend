package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicy {

    public void validateAccountCreation(UserModel user, CreateAccountRequestDTO request) {
        requireApprovedCustomer(user);
        requireValidLimits(request.absoluteLimit(), request.dailyLimit());
    }

    public void validateLimitUpdate(AccountModel account, UpdateAccountLimitsRequestDTO request) {
        requireOpenAccount(account);
        requireValidLimits(request.absoluteLimit(), request.dailyLimit());
    }

    public void requireOpenAccount(AccountModel account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalStateException("This account is closed.");
        }
    }

    public void validateAccountClosure(AccountModel account) {
        requireOpenAccount(account);
        if (account.getBalance().compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close an account with a non-zero balance.");
        }
    }

    private void requireApprovedCustomer(UserModel user) {
        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalStateException("Cannot open accounts for an unapproved customer.");
        }
    }

    private void requireValidLimits(java.math.BigDecimal absoluteLimit, java.math.BigDecimal dailyLimit) {
        if (absoluteLimit == null || dailyLimit == null) {
            throw new IllegalArgumentException("Account limits are required.");
        }
    }
}
