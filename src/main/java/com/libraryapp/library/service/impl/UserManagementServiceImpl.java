package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.ERole;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final DomainClient domainClient;

    @Override
    public UserDto updateUserRole(Long userId, ERole newRole) {
        if (domainClient.getUserById(userId) == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return updateUserReturningLatest(userId, UserDto.builder().role(newRole).build());
    }

    @Override
    public UserDto updateUserGroup(Long userId, Long userGroupId) {
        if (domainClient.getUserGroupById(userGroupId) == null) {
            throw new ResourceNotFoundException("User group not found: " + userGroupId);
        }
        if (domainClient.getUserById(userId) == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return updateUserReturningLatest(userId, UserDto.builder().userGroupId(userGroupId).build());
    }
    
    private UserDto updateUserReturningLatest(Long userId, UserDto patch) {
        UserDto updated = domainClient.updateUser(userId, patch);
        return updated != null ? updated : domainClient.getUserById(userId);
    }

    @Override
    public List<UserDto> listUsersByRole(ERole role) {
        return domainClient.findUsersByRole(role.name());
    }
}
