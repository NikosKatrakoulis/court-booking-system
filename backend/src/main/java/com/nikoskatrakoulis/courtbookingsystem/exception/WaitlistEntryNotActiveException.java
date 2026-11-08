package com.nikoskatrakoulis.courtbookingsystem.exception;

public class WaitlistEntryNotActiveException extends RuntimeException {
    public WaitlistEntryNotActiveException(String message) {
        super(message);
    }
}
