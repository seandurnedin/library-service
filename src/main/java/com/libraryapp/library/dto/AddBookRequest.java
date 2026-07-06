package com.libraryapp.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBookRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "ISBN must be exactly 13 digits")
    private String isbn;
    @NotBlank
    private String title;
    @NotBlank
    private String author;
    private String publisher;
    private Integer publishedYear;
    private String genre;
    @Min(1)
    private Integer totalCopies;
}
