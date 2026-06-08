package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.RegistrationDecisionEnum;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/registrations")
    public ResponseEntity<Page<UserDTO.RegistrationRequest>> getRegistrations(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserModel> users = "APPROVED".equalsIgnoreCase(status)
                ? userService.getActiveUsers(search, pageable)
                : userService.getPendingUsers(search, pageable);
        return ResponseEntity.ok(users.map(userMapper::toRegistrationRequest));
    }

    @PatchMapping("/registrations/{id}")
    public ResponseEntity<Void> updateRegistrationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO.UpdateRegistrationStatusRequest request
    ) {
        if (request.status() == RegistrationDecisionEnum.APPROVED) {
            userService.approveUser(id, request);
        } else {
            userService.denyUser(id);
        }
        return ResponseEntity.noContent().build();
    }
    
}
