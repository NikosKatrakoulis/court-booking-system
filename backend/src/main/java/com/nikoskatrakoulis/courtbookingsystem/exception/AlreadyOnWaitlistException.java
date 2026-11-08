package com.nikoskatrakoulis.courtbookingsystem.exception;

public class AlreadyOnWaitlistException extends RuntimeException {
    public AlreadyOnWaitlistException(String message) {
        super(message);
    }
}
