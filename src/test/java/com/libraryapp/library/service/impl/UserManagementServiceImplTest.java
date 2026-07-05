package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.UserDto;
import com.libraryapp.library.enums.ERole;
import com.libraryapp.library.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private DomainClient domainClient;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    @Test
    void updateUserRole_domainServiceReturnsBody_usesIt() {
        UserDto existing = UserDto.builder().id(1L).role(ERole.USER).build();
        when(domainClient.getUserById(1L)).thenReturn(existing);
        UserDto updated = UserDto.builder().id(1L).role(ERole.MANAGER).build();
        when(domainClient.updateUser(eq(1L), any(UserDto.class))).thenReturn(updated);

        UserDto result = userManagementService.updateUserRole(1L, ERole.MANAGER);

        assertThat(result.getRole()).isEqualTo(ERole.MANAGER);
    }

    @Test
    void updateUserRole_domainServiceReturnsEmptyBody_fallsBackToRefetch() {
        UserDto existing = UserDto.builder().id(1L).role(ERole.USER).build();
        when(domainClient.getUserById(1L)).thenReturn(existing, UserDto.builder().id(1L).role(ERole.MANAGER).build());
        when(domainClient.updateUser(eq(1L), any(UserDto.class))).thenReturn(null);

        UserDto result = userManagementService.updateUserRole(1L, ERole.MANAGER);

        assertThat(result.getRole()).isEqualTo(ERole.MANAGER);
    }

    @Test
    void updateUserRole_userNotFound_throws() {
        when(domainClient.getUserById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userManagementService.updateUserRole(1L, ERole.MANAGER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUserGroup_groupNotFound_throws() {
        when(domainClient.getUserGroupById(5L)).thenReturn(null);

        assertThatThrownBy(() -> userManagementService.updateUserGroup(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listUsersByRole_sendsRoleAsEnumName() {
        when(domainClient.findUsersByRole("MANAGER")).thenReturn(List.of(UserDto.builder().id(1L).role(ERole.MANAGER).build()));

        List<UserDto> result = userManagementService.listUsersByRole(ERole.MANAGER);

        assertThat(result).hasSize(1);
    }
}
