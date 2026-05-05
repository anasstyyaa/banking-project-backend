package inholland.nl.banking_project_backend.config;

import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> seedDatabase();
    }

    private void seedDatabase() {
        if (userRepository.count() > 0) {
            return;
        }
        System.out.println("Seeding database with test users...");
        userRepository.save(createEmployee());
        UserModel customer = userRepository.save(createCustomer());
        seedCustomerAccounts(customer);
        System.out.println("Database seeding complete.");
    }

    private UserModel createEmployee() {
        UserModel employee = baseUser("admin@inhollandbank.nl", "Admin123!");
        employee.setFirstName("Bank");
        employee.setLastName("Manager");
        employee.setPhoneNumber("+31612345678");
        employee.setRole(RoleEnum.ROLE_EMPLOYEE);
        employee.setBsn("997654321");
        return employee;
    }

    private UserModel createCustomer() {
        UserModel customer = baseUser("testuser@gmail.com", "User123!");
        customer.setFirstName("Testy");
        customer.setLastName("McTestFace");
        customer.setPhoneNumber("+31612345688");
        customer.setRole(RoleEnum.ROLE_CUSTOMER);
        customer.setBsn("987654321");
        return customer;
    }

    private UserModel baseUser(String email, String password) {
        UserModel user = new UserModel();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsApproved(true);
        return user;
    }

    private void seedCustomerAccounts(UserModel customer) {
        accountRepository.save(createAccount(customer, "NL01INHO000000001", AccountTypeEnum.CHECKING));
        accountRepository.save(createAccount(customer, "NL01INHO000000002", AccountTypeEnum.SAVINGS));
    }

    private AccountModel createAccount(UserModel user, String iban, AccountTypeEnum type) {
        AccountModel account = new AccountModel();
        account.setUser(user);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteLimit(BigDecimal.ZERO);
        account.setDailyLimit(new BigDecimal("500.00"));
        return account;
    }
}
