package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.models.UserModel;

public interface UserService {
    // Creates a new customer user with an empty customer profile.
    UserModel createCustomer(UserModel user);

    // Finds a user by email or throws a business-friendly error.
    UserModel findByEmail(String email);
}
