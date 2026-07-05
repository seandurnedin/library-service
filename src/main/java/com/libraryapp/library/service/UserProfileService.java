package com.libraryapp.library.service;

import com.libraryapp.library.dto.UserBorrowingHistoryDto;
import com.libraryapp.library.dto.UserProfileDto;

import java.util.List;

public interface UserProfileService {

    UserProfileDto getProfile(Long userId);

    List<UserBorrowingHistoryDto> getBorrowingHistory(Long userId);
}
