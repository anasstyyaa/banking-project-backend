package inholland.nl.banking_project_backend.config;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            if (args.containsOption("skip-seed")) {
                log.info("Database seeding skipped via '--skip-seed' application flag.");
                return;
            }

            if (userRepository.count() == 0) {
                log.info("Database is empty. Initiating professional demo banking dataset...");
                seedDemoData();
                log.info("Demo database seeding completed successfully.");
            } else {
                log.info("Database already contains data. Seeding bypassed.");
            }
        };
    }

    private void seedDemoData() {
        UserModel employee = createUser("admin@inhollandbank.nl", "Admin123!", "Bank", "Manager", "997654321", "+31612345678", RoleEnum.ROLE_EMPLOYEE);
        UserModel firstCustomer = createUser("testuser@gmail.com", "User123!", "Testy", "McTestFace", "987654321", "+31612345688", RoleEnum.ROLE_CUSTOMER);
        UserModel secondCustomer = createUser("jane.customer@gmail.com", "User123!", "Jane", "Customer", "987654322", "+31612345689", RoleEnum.ROLE_CUSTOMER);

        CustomerProfileModel firstProfile = createProfile(firstCustomer);
        CustomerProfileModel secondProfile = createProfile(secondCustomer);

        AccountModel firstChecking = createAccount(firstProfile, "NL01INHO000000001", AccountTypeEnum.CHECKING, "1250.00", "-500.00", "1000.00");
        AccountModel firstSavings = createAccount(firstProfile, "NL01INHO000000002", AccountTypeEnum.SAVINGS, "2500.00", "0.00", "750.00");
        AccountModel secondChecking = createAccount(secondProfile, "NL01INHO000000003", AccountTypeEnum.CHECKING, "800.00", "-250.00", "700.00");

        firstCustomer.setIban(firstChecking.getIban());
        secondCustomer.setIban(secondChecking.getIban());
        userRepository.save(firstCustomer);
        userRepository.save(secondCustomer);

        createTransaction(TransactionTypeEnum.DEPOSIT, null, firstChecking, "250.00", firstCustomer);
        createTransaction(TransactionTypeEnum.TRANSFER, firstChecking, secondChecking, "75.00", firstCustomer);
        createTransaction(TransactionTypeEnum.WITHDRAWAL, firstChecking, null, "40.00", firstCustomer);
        createTransaction(TransactionTypeEnum.TRANSFER, secondChecking, firstSavings, "50.00", employee);
    }

    // Creates and stores one demo user.
    private UserModel createUser(String email, String password, String firstName, String lastName, String bsn, String phone, RoleEnum role) {
        UserModel user = new UserModel();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBsn(bsn);
        user.setPhoneNumber(phone);
        user.setRole(role);
        user.setIsApproved(true);
        return userRepository.save(user);
    }

    // Creates and stores one customer profile for a user.
    private CustomerProfileModel createProfile(UserModel user) {
        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);
        return customerProfileRepository.save(profile);
    }

    // Creates and stores one account with configured limits.
    private AccountModel createAccount(CustomerProfileModel profile, String iban, AccountTypeEnum type, String balance, String absoluteLimit, String dailyLimit) {
        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal(balance));
        account.setAbsoluteLimit(new BigDecimal(absoluteLimit));
        account.setDailyLimit(new BigDecimal(dailyLimit));
        account.setIsActive(true);
        return accountRepository.save(account);
    }

    // Creates and stores one demo transaction record.
    private void createTransaction(TransactionTypeEnum type, AccountModel fromAccount, AccountModel toAccount, String amount, UserModel initiatedBy) {
        TransactionModel transaction = new TransactionModel();
        transaction.setType(type);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setFromIbanSnapshot(fromAccount == null ? null : fromAccount.getIban());
        transaction.setToIbanSnapshot(toAccount == null ? null : toAccount.getIban());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTimestamp(LocalDateTime.now().minusDays(1));
        transaction.setInitiatedBy(initiatedBy);
        transactionRepository.save(transaction);
    }
}