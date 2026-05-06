package inholland.nl.banking_project_backend.config;

import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            if (userRepository.count() == 0) {
                System.out.println("Seedling database with test users...");

                // test employee
                UserModel admin = new UserModel();
                admin.setEmail("admin@inhollandbank.nl");
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setFirstName("Bank");
                admin.setLastName("Manager");
                admin.setPhoneNumber("+31612345678");
                admin.setRole(RoleEnum.ROLE_EMPLOYEE);
                admin.setIsApproved(true);
                admin.setBsn("997654321");
                userRepository.save(admin);

                // test customer
                UserModel customer = new UserModel();
                customer.setEmail("testuser@gmail.com");
                customer.setPassword(passwordEncoder.encode("User123!"));
                customer.setFirstName("Testy");
                customer.setLastName("McTestFace");
                customer.setPhoneNumber("+31612345688");
                customer.setRole(RoleEnum.ROLE_CUSTOMER);
                customer.setIsApproved(true);
                customer.setBsn("987654321");
                userRepository.save(customer);

                System.out.println("Database Seeding Complete.");
            }
        };
    }
}
