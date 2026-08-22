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
            UserNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> notFoundHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @ExceptionHandler(SlotInThePastException.class)
    public ResponseEntity<ErrorResponse> badRequestHandler(SlotInThePastException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler({
            SlotAlreadyBookedException.class,
            CourtNotAvailableException.class,
            CourtTypeNotAvailableException.class
    })
    public ResponseEntity<ErrorResponse> conflictHandler(RuntimeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

}
