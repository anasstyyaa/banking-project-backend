package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDTO.ProfileResponse> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

   @PutMapping("/profile")
    public ResponseEntity<UserDTO.ProfileResponse> updateProfile(
            @Valid @RequestBody UserDTO.UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
}
