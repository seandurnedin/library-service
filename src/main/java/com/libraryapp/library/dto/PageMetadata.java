package com.libraryapp.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadata {
    
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
}
