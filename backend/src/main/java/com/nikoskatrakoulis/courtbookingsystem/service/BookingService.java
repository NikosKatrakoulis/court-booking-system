package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.entity.*;
import com.nikoskatrakoulis.courtbookingsystem.exception.*;
import com.nikoskatrakoulis.courtbookingsystem.repository.BookingRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.SlotRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createBooking(Long userId, Long slotId) {

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));


        LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getStartTime());
        if (slotDateTime.isBefore(LocalDateTime.now())) {
            throw new SlotInThePastException("Cannot book a slot in the past");
        }

        if (!slot.getCourt().isActive()) {
            throw new CourtNotAvailableException("Court is not available for booking");
        }

        if (!slot.getCourt().getCourtType().isActive()) {
            throw new CourtTypeNotAvailableException("Court type is not available for booking");
        }

        Slot lockedSlot = slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found with id: " + slotId));

        Optional<Booking> existingBooking = bookingRepository.findBySlot_IdAndStatus(slotId, Status.CONFIRMED);
        if (existingBooking.isPresent()) {
            throw new SlotAlreadyBookedException("Slot is already booked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Booking newBooking = new Booking();
        newBooking.setUser(user);
        newBooking.setSlot(lockedSlot);
        newBooking.setStatus(Status.CONFIRMED);

        bookingRepository.save(newBooking);
    }
}
