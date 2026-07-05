package com.libraryapp.library.service;

import com.libraryapp.library.dto.BorrowingRecordDto;

import java.util.List;

public interface LendingService {

    BorrowingRecordDto loanBook(String username, String isbn);

    BorrowingRecordDto returnBook(String username, String isbn);

    List<BorrowingRecordDto> getLoanedBooks(Long userId);
}
