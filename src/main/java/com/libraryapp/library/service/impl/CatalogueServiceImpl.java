package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.*;
import com.libraryapp.library.enums.EBookStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.DuplicateResourceException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import com.libraryapp.library.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CatalogueServiceImpl implements CatalogueService {

    private final DomainClient domainClient;

    @Override
    public BookDto addBook(AddBookRequest request) {
        BookDto existing = domainClient.findBookByIsbn(request.getIsbn());
        if (existing != null) {
            if (!matchesExistingBook(existing, request)) {
                throw new DuplicateResourceException(
                        "A book with ISBN " + request.getIsbn() + " already exists with different details");
            }
            return addCopiesToExistingBook(existing, request.getTotalCopies() == null ? 1 : request.getTotalCopies());
        }

        int copies = request.getTotalCopies() == null ? 1 : request.getTotalCopies();
        BookDto newBook = BookDto.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publishedYear(request.getPublishedYear())
                .genre(request.getGenre())
                .totalCopies(copies)
                .availableCopies(copies)
                .status(EBookStatus.IN_STORE)
                .build();
        return domainClient.createBook(newBook);
    }
    
    private boolean matchesExistingBook(BookDto existing, AddBookRequest request) {
        return textEquals(existing.getTitle(), request.getTitle())
                && textEquals(existing.getAuthor(), request.getAuthor())
                && textEquals(existing.getPublisher(), request.getPublisher())
                && Objects.equals(existing.getPublishedYear(), request.getPublishedYear())
                && textEquals(existing.getGenre(), request.getGenre());
    }

    private boolean textEquals(String a, String b) {
        String normalizedA = a == null ? "" : a.trim().toLowerCase();
        String normalizedB = b == null ? "" : b.trim().toLowerCase();
        return normalizedA.equals(normalizedB);
    }

    private BookDto addCopiesToExistingBook(BookDto existing, int additionalCopies) {
        int currentTotal = existing.getTotalCopies() != null ? existing.getTotalCopies() : 0;
        int currentAvailable = existing.getAvailableCopies() != null ? existing.getAvailableCopies() : 0;
        BookDto patch = BookDto.builder()
                .totalCopies(currentTotal + additionalCopies)
                .availableCopies(currentAvailable + additionalCopies)
                .status(EBookStatus.IN_STORE)
                .build();
        return domainClient.updateBook(existing.getId(), patch);
    }

    @Override
    public void removeBook(Long bookId) {
        if (domainClient.getBookById(bookId) == null) {
            throw new ResourceNotFoundException("Book not found: " + bookId);
        }
        domainClient.deleteBook(bookId);
    }

    @Override
    public BookDto updateBook(Long bookId, UpdateBookRequest request) {
        BookDto existing = domainClient.getBookById(bookId);
        if (existing == null) {
            throw new ResourceNotFoundException("Book not found: " + bookId);
        }

        BookDto.BookDtoBuilder patch = BookDto.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publishedYear(request.getPublishedYear())
                .genre(request.getGenre());

        if (request.getTotalCopies() != null) {
            applyTotalCopiesChange(existing, request.getTotalCopies(), patch);
        }

        return domainClient.updateBook(bookId, patch.build());
    }

    private void applyTotalCopiesChange(BookDto existing, int newTotalCopies, BookDto.BookDtoBuilder patch) {
        int currentTotal = existing.getTotalCopies() != null ? existing.getTotalCopies() : 0;
        int currentAvailable = existing.getAvailableCopies() != null ? existing.getAvailableCopies() : 0;
        int copiesOnLoan = currentTotal - currentAvailable;

        if (newTotalCopies < copiesOnLoan) {
            throw new BusinessRuleException(
                    "Cannot reduce total copies to " + newTotalCopies + " - " + copiesOnLoan
                            + (copiesOnLoan == 1 ? " copy is" : " copies are") + " currently on loan");
        }

        int delta = newTotalCopies - currentTotal;
        patch.totalCopies(newTotalCopies)
                .availableCopies(currentAvailable + delta);
    }

    @Override
    public BookDto getBook(Long bookId) {
        BookDto book = domainClient.getBookById(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found: " + bookId);
        }
        return book;
    }

    @Override
    public PageResponse<BookDto> getAllBooks(Pageable pageable) {
        SortSpec sort = resolveSort(pageable);
        HalPage<BookDto> halPage = domainClient.getBooksPage(pageable.getPageNumber(), pageable.getPageSize(), sort.field(), sort.direction());
        return toPageResponse(halPage, pageable);
    }

    @Override
    public PageResponse<BookDto> searchBooks(String title, Pageable pageable) {
        SortSpec sort = resolveSort(pageable);
        HalPage<BookDto> halPage = domainClient.searchBooksPage(title, pageable.getPageNumber(), pageable.getPageSize(), sort.field(), sort.direction());
        return toPageResponse(halPage, pageable);
    }

    private SortSpec resolveSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            return new SortSpec(order.getProperty(), order.getDirection().name().toLowerCase());
        }
        return new SortSpec("title", "asc");
    }

    private PageResponse<BookDto> toPageResponse(HalPage<BookDto> halPage, Pageable pageable) {
        PageMetadata meta = halPage.getPage();
        return PageResponse.<BookDto>builder()
                .content(halPage.content())
                .page(meta != null && meta.getNumber() != null ? meta.getNumber() : pageable.getPageNumber())
                .size(meta != null && meta.getSize() != null ? meta.getSize() : pageable.getPageSize())
                .totalElements(meta != null && meta.getTotalElements() != null ? meta.getTotalElements() : (long) halPage.content().size())
                .totalPages(meta != null && meta.getTotalPages() != null ? meta.getTotalPages() : 1)
                .build();
    }

    private record SortSpec(String field, String direction) {
    }
}
