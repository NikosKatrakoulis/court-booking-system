package com.nikoskatrakoulis.courtbookingsystem.exception;

public class BookingNotOwnedException extends RuntimeException {
    public BookingNotOwnedException(String message) {
        super(message);
    }
}
