package com.nikoskatrakoulis.courtbookingsystem.exception;

public class CourtNotAvailableException extends RuntimeException {
    public CourtNotAvailableException(String message) {
        super(message);
    }
}
