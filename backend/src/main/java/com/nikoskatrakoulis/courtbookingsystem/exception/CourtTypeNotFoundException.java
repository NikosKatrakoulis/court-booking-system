package com.nikoskatrakoulis.courtbookingsystem.exception;

public class CourtTypeNotFoundException extends RuntimeException {
    public CourtTypeNotFoundException(String message) {
        super(message);
    }
}
