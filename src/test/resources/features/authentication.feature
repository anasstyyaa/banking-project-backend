Feature: Authentication

  Scenario: Approved employee logs in successfully
    Given an approved "ROLE_EMPLOYEE" user exists with email "employee-bdd@example.com" and password "Admin123!"
    When the user logs in with email "employee-bdd@example.com" and password "Admin123!"
    Then the response status should be 200
    And the response should contain a JWT token
    And the response role should be "ROLE_EMPLOYEE"

  Scenario: Login fails with a wrong password
    Given an approved "ROLE_CUSTOMER" user exists with email "wrong-password-bdd@example.com" and password "User123!"
    When the user logs in with email "wrong-password-bdd@example.com" and password "Wrong123!"
    Then the response status should be 401

  Scenario: Pending customer logs in with pending status
    Given a pending "ROLE_CUSTOMER" user exists with email "pending-bdd@example.com" and password "User123!"
    When the user logs in with email "pending-bdd@example.com" and password "User123!"
    Then the response status should be 200
    And the response approval flag should be false
