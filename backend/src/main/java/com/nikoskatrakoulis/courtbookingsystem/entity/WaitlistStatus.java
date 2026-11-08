package com.nikoskatrakoulis.courtbookingsystem.entity;

public enum WaitlistStatus {
    WAITING,    // in the queue
    OFFERED,    // slot is reserved for this user until offerExpiresAt
    FULFILLED,  // user booked the slot
    EXPIRED,    // offer was not taken in time
    LEFT        // user left the queue
}
