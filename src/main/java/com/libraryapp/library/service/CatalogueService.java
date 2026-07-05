package com.libraryapp.library.service;

import com.libraryapp.library.dto.AddBookRequest;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.PageResponse;
import com.libraryapp.library.dto.UpdateBookRequest;
import org.springframework.data.domain.Pageable;

public interface CatalogueService {

    // (MANAGER)
    BookDto addBook(AddBookRequest request);

    // (MANAGER)
    void removeBook(Long bookId);

    // (MANAGER)
    BookDto updateBook(Long bookId, UpdateBookRequest request);

    BookDto getBook(Long bookId);

    PageResponse<BookDto> getAllBooks(Pageable pageable);

    /**
     * For searchbar functionality
     */
    PageResponse<BookDto> searchBooks(String title, Pageable pageable);
}
