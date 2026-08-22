package com.nikoskatrakoulis.courtbookingsystem.exception;

public class SlotInThePastException extends RuntimeException {
    public SlotInThePastException(String message) {
        super(message);
    }
}
