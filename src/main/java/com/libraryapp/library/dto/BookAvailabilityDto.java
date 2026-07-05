package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookAvailabilityDto {

    private Long bookId;
    private String title;
    private String isbn;
    private String status;
    private Integer availableCopies;
    private Integer totalCopies;
    private Integer queueLength;
    private Long nextInQueueUserId;
}
