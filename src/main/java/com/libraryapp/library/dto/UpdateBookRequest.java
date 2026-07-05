package com.libraryapp.library.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {

    private String title;
    private String author;
    private String publisher;
    private Integer publishedYear;
    private String genre;
    @Min(0)
    private Integer totalCopies;
}
