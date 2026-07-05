package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.UserBorrowingHistoryDto;
import com.libraryapp.library.dto.UserProfileDto;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final DomainClient domainClient;

    @Override
    public UserProfileDto getProfile(Long userId) {
        UserProfileDto profile = domainClient.getUserProfile(userId);
        if (profile == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return profile;
    }

    @Override
    public List<UserBorrowingHistoryDto> getBorrowingHistory(Long userId) {
        return domainClient.getUserBorrowingHistory(userId);
    }
}
