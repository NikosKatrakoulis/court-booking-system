package com.nikoskatrakoulis.courtbookingsystem.exception;

public class CourtTypeNotAvailableException extends RuntimeException {
    public CourtTypeNotAvailableException(String message) {
        super(message);
    }
}
