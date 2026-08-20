package com.nikoskatrakoulis.courtbookingsystem.exception;

public class CourtNotFoundException extends RuntimeException {
    public CourtNotFoundException(String message) {
        super(message);
    }
}
