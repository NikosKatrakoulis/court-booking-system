package com.nikoskatrakoulis.courtbookingsystem.repository.projection;

import java.time.LocalTime;

public interface SlotAvailabilityProjection {

    Long getSlotId();

    LocalTime getStartTime();

    LocalTime getEndTime();

    Long getBookingId();
}
