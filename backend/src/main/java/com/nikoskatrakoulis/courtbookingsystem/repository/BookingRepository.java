package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
