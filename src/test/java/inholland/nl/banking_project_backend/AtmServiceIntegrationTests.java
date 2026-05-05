package inholland.nl.banking_project_backend;

import inholland.nl.banking_project_backend.dtos.AtmDTO;
import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.TransactionTypeEnum;
import inholland.nl.banking_project_backend.services.AtmService;
import inholland.nl.banking_project_backend.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AtmServiceIntegrationTests {
    @Autowired
    private AuthService authService;

    @Autowired
    private AtmService atmService;

    @Test
    void customerCanUseAtmAfterNormalLogin() {
        UserDTO.LoginResponse login = loginCustomer();
        AtmDTO.AccountResponse account = firstAccount(login.email());

        AtmDTO.TransactionResponse deposit = deposit(login.email(), account.id());
        AtmDTO.TransactionResponse withdrawal = withdraw(login.email(), account.id());

        assertFalse(login.token().isBlank());
        assertEquals(TransactionTypeEnum.ATM_DEPOSIT, deposit.type());
        assertEquals(TransactionTypeEnum.ATM_WITHDRAWAL, withdrawal.type());
    }

    private UserDTO.LoginResponse loginCustomer() {
        return authService.login(new UserDTO.LoginRequest("testuser@gmail.com", "User123!"));
    }

    private AtmDTO.AccountResponse firstAccount(String email) {
        return atmService.getAccounts(email).getFirst();
    }

    private AtmDTO.TransactionResponse deposit(String email, Long accountId) {
        return atmService.deposit(email, new AtmDTO.MoneyRequest(accountId, new BigDecimal("25.00")));
    }

    private AtmDTO.TransactionResponse withdraw(String email, Long accountId) {
        return atmService.withdraw(email, new AtmDTO.MoneyRequest(accountId, new BigDecimal("10.00")));
    }
}
