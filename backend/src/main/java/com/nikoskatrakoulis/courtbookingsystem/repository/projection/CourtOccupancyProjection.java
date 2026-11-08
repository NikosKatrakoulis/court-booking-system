package com.nikoskatrakoulis.courtbookingsystem.repository.projection;

public interface CourtOccupancyProjection {
    Long getCourtId();
    String getCourtName();
    Long getTotalSlots();
    Long getBookedSlots();
}
