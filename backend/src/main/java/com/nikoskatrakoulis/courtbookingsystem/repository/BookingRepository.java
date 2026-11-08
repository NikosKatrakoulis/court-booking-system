package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Booking;
import com.nikoskatrakoulis.courtbookingsystem.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

     Optional<Booking> findBySlot_IdAndStatus(Long id, Status status);

     /** Reads only the slot id, so the booking is NOT loaded into the persistence context before the slot is locked. */
     @Query("select b.slot.id from Booking b where b.id = :id")
     Optional<Long> findSlotIdById(@Param("id") Long id);

}
