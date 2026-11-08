package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Booking;
import com.nikoskatrakoulis.courtbookingsystem.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.projection.PopularSlotProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

     Optional<Booking> findBySlot_IdAndStatus(Long id, Status status);

     /** Most booked start times (CONFIRMED only) in a date range; pass a PageRequest to limit the result. */
     @Query("""
                SELECT s.startTime AS startTime, COUNT(b.id) AS bookings
                FROM Booking b
                JOIN b.slot s
                WHERE b.status = 'CONFIRMED' AND s.date BETWEEN :from AND :to
                GROUP BY s.startTime
                ORDER BY COUNT(b.id) DESC, s.startTime ASC
            """)
     List<PopularSlotProjection> findPopularStartTimes(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

     /** Reads only the slot id, so the booking is NOT loaded into the persistence context before the slot is locked. */
     @Query("select b.slot.id from Booking b where b.id = :id")
     Optional<Long> findSlotIdById(@Param("id") Long id);

}
