package com.libraryapp.library.controller;

import com.libraryapp.library.dto.UpdateUserGroupRequest;
import com.libraryapp.library.dto.UpdateUserRoleRequest;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.ERole;
import com.libraryapp.library.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "User role and user-group administration (ADMIN only)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserManagementService userManagementService;

    @Operation(summary = "Change a user's role")
    @PutMapping("/users/{userId}/role")
    public UserDto updateRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        return userManagementService.updateUserRole(userId, request.getRole());
    }

    @Operation(summary = "Move a user to a different user group")
    @PutMapping("/users/{userId}/group")
    public UserDto updateGroup(@PathVariable Long userId, @Valid @RequestBody UpdateUserGroupRequest request) {
        return userManagementService.updateUserGroup(userId, request.getUserGroupId());
    }

    @Operation(summary = "List all users with a given role")
    @GetMapping("/users")
    public List<UserDto> listByRole(@RequestParam ERole role) {
        return userManagementService.listUsersByRole(role);
    }
}
