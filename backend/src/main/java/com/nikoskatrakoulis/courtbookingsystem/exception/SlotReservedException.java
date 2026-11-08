package com.nikoskatrakoulis.courtbookingsystem.exception;

public class SlotReservedException extends RuntimeException {
    public SlotReservedException(String message) {
        super(message);
    }
}
