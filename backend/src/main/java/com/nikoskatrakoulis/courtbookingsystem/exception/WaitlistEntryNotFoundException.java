package com.nikoskatrakoulis.courtbookingsystem.exception;

public class WaitlistEntryNotFoundException extends RuntimeException {
    public WaitlistEntryNotFoundException(String message) {
        super(message);
    }
}
