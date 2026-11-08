package com.nikoskatrakoulis.courtbookingsystem.exception;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.ErrorResponse;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        Map<String, String> errors = fieldErrors.stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrorResponse);
    }

    @ExceptionHandler({
            CourtNotFoundException.class,
            CourtTypeNotFoundException.class,
            SlotNotFoundException.class,
            UserNotFoundException.class,
            BookingNotFoundException.class,
            WaitlistEntryNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> notFoundHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @ExceptionHandler({SlotInThePastException.class, InvalidDateRangeException.class, InvalidIdempotencyKeyException.class})
    public ResponseEntity<ErrorResponse> badRequestHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler({
            SlotAlreadyBookedException.class,
            CourtNotAvailableException.class,
            CourtTypeNotAvailableException.class,
            BookingAlreadyCancelledException.class,
            CancellationNotAllowedException.class,
            EmailAlreadyExistsException.class,
            AlreadyOnWaitlistException.class,
            SlotStillAvailableException.class,
            SlotReservedException.class,
            WaitlistEntryNotActiveException.class,
            IdempotencyKeyReusedException.class
    })
    public ResponseEntity<ErrorResponse> conflictHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            InvalidRefreshTokenException.class
    })
    public ResponseEntity<ErrorResponse> unauthorizeHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler({BookingNotOwnedException.class, WaitlistEntryNotOwnedException.class})
    public ResponseEntity<ErrorResponse> forbiddenHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

}
