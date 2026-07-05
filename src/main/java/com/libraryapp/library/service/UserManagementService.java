package com.libraryapp.library.service;

import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.ERole;

import java.util.List;

// ADMIN
public interface UserManagementService {

    UserDto updateUserRole(Long userId, ERole newRole);

    UserDto updateUserGroup(Long userId, Long userGroupId);

    List<UserDto> listUsersByRole(ERole role);
}
