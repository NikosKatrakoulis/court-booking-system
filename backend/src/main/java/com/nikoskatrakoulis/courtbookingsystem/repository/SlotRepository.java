package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Slot;
import com.nikoskatrakoulis.courtbookingsystem.repository.projection.SlotAvailabilityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("""
                SELECT s.id AS slotId, s.startTime AS startTime, s.endTime AS endTime,
                       b.id AS bookingId
                FROM Slot s
                LEFT JOIN Booking b ON b.slot.id = s.id AND b.status = 'CONFIRMED'
                WHERE s.court.id = :courtId AND s.date = :date
            """)
    List<SlotAvailabilityProjection> findAvailableSlots(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );
}
