package com.nikoskatrakoulis.courtbookingsystem.exception;

public class WaitlistEntryNotOwnedException extends RuntimeException {
    public WaitlistEntryNotOwnedException(String message) {
        super(message);
    }
}
