package com.libraryapp.library.controller;

import com.libraryapp.library.dto.ReservationDto;
import com.libraryapp.library.dto.ReserveRequest;
import com.libraryapp.library.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservations", description = "Waitlist for books that are fully checked out")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Join the waitlist for a book that's currently fully loaned out")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReservationDto reserve(@Valid @RequestBody ReserveRequest request, HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        return reservationService.reserveBook(userId, request.getBookId());
    }

    @Operation(summary = "Cancel one of my reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.cancelReservation(userId, id);
    }

    @Operation(summary = "View my wishlist (active reservations)")
    @GetMapping("/my-wishlist")
    public List<ReservationDto> myWishlist(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return reservationService.getWishlist(userId);
    }

    @Operation(summary = "View every active reservation for a book (MANAGER)")
    @GetMapping("/book/{bookId}")
    public List<ReservationDto> reservationsForBook(@PathVariable Long bookId) {
        return reservationService.getReservationsForBook(bookId);
    }

    @Operation(summary = "Cancel any member's reservation for a book on their behalf (MANAGER)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/book/{bookId}/{reservationId}")
    public void cancelForBook(@PathVariable Long bookId, @PathVariable Long reservationId) {
        reservationService.cancelReservationForBook(bookId, reservationId);
    }
}
