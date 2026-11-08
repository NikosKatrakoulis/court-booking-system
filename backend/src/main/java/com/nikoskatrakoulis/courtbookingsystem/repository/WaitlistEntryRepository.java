package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.WaitlistEntry;
import com.nikoskatrakoulis.courtbookingsystem.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    /** FIFO: oldest waiting entry first (id breaks ties). */
    Optional<WaitlistEntry> findFirstBySlot_IdAndStatusOrderByCreatedAtAscIdAsc(Long slotId, WaitlistStatus status);

    /** The outstanding, not-yet-expired offer for a slot, if any. */
    Optional<WaitlistEntry> findFirstBySlot_IdAndStatusAndOfferExpiresAtAfter(Long slotId, WaitlistStatus status, LocalDateTime now);

    Optional<WaitlistEntry> findByUser_IdAndSlot_IdAndStatusIn(Long userId, Long slotId, Collection<WaitlistStatus> statuses);

    List<WaitlistEntry> findByUser_IdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<WaitlistStatus> statuses);

    @Query("select e.id from WaitlistEntry e where e.status = :status and e.offerExpiresAt < :now")
    List<Long> findIdsByStatusAndOfferExpiredBefore(@Param("status") WaitlistStatus status, @Param("now") LocalDateTime now);

    /** Reads only the slot id, so the entry is NOT loaded into the persistence context before we lock the slot. */
    @Query("select e.slot.id from WaitlistEntry e where e.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);
}
