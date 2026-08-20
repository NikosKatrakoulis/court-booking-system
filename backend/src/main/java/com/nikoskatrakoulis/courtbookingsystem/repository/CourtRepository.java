package com.nikoskatrakoulis.courtbookingsystem.repository;

import com.nikoskatrakoulis.courtbookingsystem.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByCourtType_Name(String name);

    List<Court> findByIsActiveTrue();
}
