package inholland.nl.banking_project_backend.mapper;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceMapper {
    // Converts an account entity into a frontend-safe account response.
    public AccountDTO.AccountResponse toResponse(AccountModel account) {
        return new AccountDTO.AccountResponse(
                account.getIban(),
                account.getType(),
                account.getBalance(),
                account.getIsActive(),
                account.getAbsoluteLimit(),
                account.getDailyLimit(),
                account.getCustomer().getUser().getEmail(),
                getCustomerName(account)
        );
    }

    // Builds the customer's display name from the account owner.
    private String getCustomerName(AccountModel account) {
        return account.getCustomer().getUser().getFirstName() + " " + account.getCustomer().getUser().getLastName();
    }
}
