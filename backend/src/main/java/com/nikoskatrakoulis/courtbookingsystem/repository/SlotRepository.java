package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Slot;
import com.nikoskatrakoulis.courtbookingsystem.repository.projection.CourtOccupancyProjection;
import com.nikoskatrakoulis.courtbookingsystem.repository.projection.SlotAvailabilityProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    /** Slots vs CONFIRMED bookings per court in a date range. LEFT JOIN keeps unbooked slots in the count. */
    @Query("""
                SELECT c.id AS courtId, c.name AS courtName,
                       COUNT(s.id) AS totalSlots, COUNT(b.id) AS bookedSlots
                FROM Slot s
                JOIN s.court c
                LEFT JOIN Booking b ON b.slot.id = s.id AND b.status = 'CONFIRMED'
                WHERE s.date BETWEEN :from AND :to
                GROUP BY c.id, c.name
                ORDER BY c.name
            """)
    List<CourtOccupancyProjection> findOccupancy(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Slot> findByCourt_IdAndDateBetween(Long courtId, LocalDate from, LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT s
                FROM Slot s
                WHERE s.id = :id
            """)
    Optional<Slot> findByIdWithLock(
            @Param("id") Long id
    );
}
