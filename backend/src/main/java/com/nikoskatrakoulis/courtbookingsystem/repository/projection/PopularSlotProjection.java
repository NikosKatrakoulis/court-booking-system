package com.nikoskatrakoulis.courtbookingsystem.repository.projection;

import java.time.LocalTime;

public interface PopularSlotProjection {
    LocalTime getStartTime();
    Long getBookings();
}
