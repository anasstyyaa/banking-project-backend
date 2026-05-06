package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.services.UserSerivce;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserSerivce userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDTO.ProfileResponse> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @PutMapping("/profile/phone-number")
    public ResponseEntity<UserDTO.ProfileResponse> updatePhoneNumber(
            Principal principal,
            @Valid @RequestBody UserDTO.UpdatePhoneNumberRequest request
    ) {
        return ResponseEntity.ok(userService.updatePhoneNumber(principal.getName(), request));
    }
}
<<<<<<< HEAD
//url hacer mas general
=======
>>>>>>> parent of ebea173 (Revert "qwerty")
