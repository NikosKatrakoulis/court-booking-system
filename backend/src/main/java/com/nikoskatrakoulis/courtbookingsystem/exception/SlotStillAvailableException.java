package com.nikoskatrakoulis.courtbookingsystem.exception;

public class SlotStillAvailableException extends RuntimeException {
    public SlotStillAvailableException(String message) {
        super(message);
    }
}
