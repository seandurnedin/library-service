package com.libraryapp.library.service.impl;

import com.libraryapp.library.client.DomainClient;
import com.libraryapp.library.dto.AddBookRequest;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.HalPage;
import com.libraryapp.library.dto.PageMetadata;
import com.libraryapp.library.dto.PageResponse;
import com.libraryapp.library.dto.UpdateBookRequest;
import com.libraryapp.library.enums.EBookStatus;
import com.libraryapp.library.exception.BusinessRuleException;
import com.libraryapp.library.exception.DuplicateResourceException;
import com.libraryapp.library.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogueServiceImplTest {

    @Mock
    private DomainClient domainClient;

    @InjectMocks
    private CatalogueServiceImpl catalogueService;

    @Test
    void addBook_newIsbn_createsBook() {
        AddBookRequest request = AddBookRequest.builder()
                .isbn("978-1").title("Clean Code").author("Robert Martin").totalCopies(2)
                .build();
        when(domainClient.findBookByIsbn("978-1")).thenReturn(null);

        BookDto created = BookDto.builder().id(1L).isbn("978-1").title("Clean Code")
                .totalCopies(2).availableCopies(2).status(EBookStatus.IN_STORE).build();
        when(domainClient.createBook(any(BookDto.class))).thenReturn(created);

        BookDto book = catalogueService.addBook(request);

        assertThat(book.getId()).isEqualTo(1L);
        verify(domainClient).createBook(argThat(dto ->
                dto.getAvailableCopies() == 2 && dto.getTotalCopies() == 2 && dto.getStatus() == EBookStatus.IN_STORE));
    }

    @Test
    void addBook_duplicateIsbn_throws() {
        AddBookRequest request = AddBookRequest.builder().isbn("978-1").title("X").author("Y").build();
        BookDto existing = BookDto.builder().id(1L).isbn("978-1").build();
        when(domainClient.findBookByIsbn("978-1")).thenReturn(existing);

        assertThatThrownBy(() -> catalogueService.addBook(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(domainClient, never()).createBook(any());
    }

    @Test
    void removeBook_notFound_throws() {
        when(domainClient.getBookById(99L)).thenReturn(null);

        assertThatThrownBy(() -> catalogueService.removeBook(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(domainClient, never()).deleteBook(any());
    }

    @Test
    void removeBook_found_deletes() {
        BookDto book = BookDto.builder().id(1L).build();
        when(domainClient.getBookById(1L)).thenReturn(book);

        catalogueService.removeBook(1L);

        verify(domainClient).deleteBook(1L);
    }

    @Test
    void updateBook_notFound_throws() {
        when(domainClient.getBookById(99L)).thenReturn(null);

        assertThatThrownBy(() -> catalogueService.updateBook(99L, UpdateBookRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(domainClient, never()).updateBook(any(), any());
    }

    @Test
    void updateBook_noTotalCopiesChange_leavesAvailableCopiesUntouched() {
        BookDto existing = BookDto.builder().id(1L).totalCopies(5).availableCopies(3).build();
        when(domainClient.getBookById(1L)).thenReturn(existing);
        when(domainClient.updateBook(eq(1L), any(BookDto.class))).thenReturn(existing);

        UpdateBookRequest request = UpdateBookRequest.builder().title("New Title").build();
        catalogueService.updateBook(1L, request);

        verify(domainClient).updateBook(eq(1L), argThat(patch ->
                patch.getTotalCopies() == null && patch.getAvailableCopies() == null));
    }

    @Test
    void updateBook_increasingTotalCopies_increasesAvailableCopiesBySameDelta() {
        BookDto existing = BookDto.builder().id(1L).totalCopies(5).availableCopies(3).build();
        when(domainClient.getBookById(1L)).thenReturn(existing);
        when(domainClient.updateBook(eq(1L), any(BookDto.class))).thenReturn(existing);

        UpdateBookRequest request = UpdateBookRequest.builder().totalCopies(8).build();
        catalogueService.updateBook(1L, request);

        // 2 copies on loan (5 - 3); raising total to 8 should raise available to 6 (8 - 2)
        verify(domainClient).updateBook(eq(1L), argThat(patch ->
                patch.getTotalCopies() == 8 && patch.getAvailableCopies() == 6));
    }

    @Test
    void updateBook_reducingTotalCopiesAboveOnLoanCount_reducesAvailableCopies() {
        BookDto existing = BookDto.builder().id(1L).totalCopies(10).availableCopies(8).build();
        when(domainClient.getBookById(1L)).thenReturn(existing);
        when(domainClient.updateBook(eq(1L), any(BookDto.class))).thenReturn(existing);

        UpdateBookRequest request = UpdateBookRequest.builder().totalCopies(3).build();
        catalogueService.updateBook(1L, request);

        // 2 copies on loan (10 - 8); reducing total to 3 should reduce available to 1 (3 - 2)
        verify(domainClient).updateBook(eq(1L), argThat(patch ->
                patch.getTotalCopies() == 3 && patch.getAvailableCopies() == 1));
    }

    @Test
    void updateBook_reducingTotalCopiesBelowOnLoanCount_throwsBusinessRule() {
        BookDto existing = BookDto.builder().id(1L).totalCopies(10).availableCopies(8).build();
        when(domainClient.getBookById(1L)).thenReturn(existing);

        // 2 copies on loan (10 - 8); can't reduce total below that
        UpdateBookRequest request = UpdateBookRequest.builder().totalCopies(1).build();

        assertThatThrownBy(() -> catalogueService.updateBook(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(domainClient, never()).updateBook(any(), any());
    }

    @Test
    void getAllBooks_mapsHalPageToPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        BookDto b1 = BookDto.builder().id(1L).title("A").build();
        HalPage<BookDto> halPage = HalPage.<BookDto>builder()
                .embedded(Map.of("books", List.of(b1)))
                .page(PageMetadata.builder().size(10).totalElements(1L).totalPages(1).number(0).build())
                .build();
        when(domainClient.getBooksPage(0, 10, "title", "asc")).thenReturn(halPage);

        PageResponse<BookDto> page = catalogueService.getAllBooks(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchBooks_delegatesToSearchEndpointWithSameSortDefaults() {
        Pageable pageable = PageRequest.of(0, 10);
        BookDto match = BookDto.builder().id(2L).title("Effective Java").build();
        HalPage<BookDto> halPage = HalPage.<BookDto>builder()
                .embedded(Map.of("books", List.of(match)))
                .page(PageMetadata.builder().size(10).totalElements(1L).totalPages(1).number(0).build())
                .build();
        when(domainClient.searchBooksPage("java", 0, 10, "title", "asc")).thenReturn(halPage);

        PageResponse<BookDto> page = catalogueService.searchBooks("java", pageable);

        assertThat(page.getContent()).containsExactly(match);
    }
}
