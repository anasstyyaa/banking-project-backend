package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.stereotype.Component;

@Component
public class AccountPolicy {

    // Ensures only approved customers can receive created accounts.
    public void requireApprovedCustomer(UserModel user) {
        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalStateException("Cannot open accounts for an unapproved customer.");
        }
    }

    // Ensures employee limit updates only affect accounts that have not been closed.
    public void requireOpenAccount(AccountModel account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new InactiveAccountException("This account is closed.");
        }
    }
}
