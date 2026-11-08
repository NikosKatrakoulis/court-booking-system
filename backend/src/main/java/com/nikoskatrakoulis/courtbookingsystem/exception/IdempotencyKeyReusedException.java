package com.nikoskatrakoulis.courtbookingsystem.exception;

/** Same Idempotency-Key sent with a DIFFERENT request (another slot): a client bug, never silently accepted. */
public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(String message) {
        super(message);
    }
}
