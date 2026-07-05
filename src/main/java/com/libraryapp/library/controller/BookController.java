package com.libraryapp.library.controller;

import com.libraryapp.library.dto.AddBookRequest;
import com.libraryapp.library.dto.BookDto;
import com.libraryapp.library.dto.PageResponse;
import com.libraryapp.library.dto.UpdateBookRequest;
import com.libraryapp.library.service.CatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Books", description = "Catalogue browsing and management")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final CatalogueService catalogueService;

    @Operation(summary = "Browse the full catalogue, or search by title (any authenticated user)")
    @GetMapping
    public PageResponse<BookDto> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        if (search != null && !search.isBlank()) {
            return catalogueService.searchBooks(search.trim(), pageable);
        }
        return catalogueService.getAllBooks(pageable);
    }
    
    @Operation(summary = "Get a single book by id")
    @GetMapping("/{id}")
    public BookDto getBook(@PathVariable Long id) {
        return catalogueService.getBook(id);
    }

    @Operation(summary = "Add a new title to the catalogue (MANAGER)")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BookDto addBook(@Valid @RequestBody AddBookRequest request) {
        return catalogueService.addBook(request);
    }

    @Operation(summary = "Update a book's catalogue details (MANAGER)")
    @PutMapping("/{id}")
    public BookDto updateBook(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest request) {
        return catalogueService.updateBook(id, request);
    }

    @Operation(summary = "Remove a book from the catalogue (MANAGER)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void removeBook(@PathVariable Long id) {
        catalogueService.removeBook(id);
    }
}
