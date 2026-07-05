package com.libraryapp.library.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private Long userId;
    private String type;
    private String message;
    private Long bookId;
    private String bookTitle;
    private Boolean read;

    @JsonAlias("createdAt")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
