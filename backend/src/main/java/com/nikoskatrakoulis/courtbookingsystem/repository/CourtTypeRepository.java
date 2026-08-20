package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.CourtType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtTypeRepository extends JpaRepository<CourtType, Long> {
}
