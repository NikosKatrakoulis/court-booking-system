package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtUpdateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.Court;
import com.nikoskatrakoulis.courtbookingsystem.entity.CourtType;
import com.nikoskatrakoulis.courtbookingsystem.exception.CourtNotFoundException;
import com.nikoskatrakoulis.courtbookingsystem.exception.CourtTypeNotFoundException;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtRepository;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final CourtTypeRepository courtTypeRepository;

    public List<CourtResponse> getAllCourts() {
        List<Court> courts = courtRepository.findAll();
        return courts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CourtResponse getCourtById(Long id) {
        Court court = findCourtOrThrow(id);
        return toResponse(court);
    }

    public CourtResponse createCourt(CourtCreateRequest request) {
        Court court = toEntity(request);
        Court savedCourt = courtRepository.save(court);
        return toResponse(savedCourt);
    }

    public CourtResponse updateCourtById(Long id, CourtUpdateRequest request) {
        Court existingCourt = findCourtOrThrow(id);

        if (request.getName() != null) {
            existingCourt.setName(request.getName());
        }
        if (request.getCourtTypeId() != null) {
            CourtType courtType = findCourtTypeOrThrow(request.getCourtTypeId());
            existingCourt.setCourtType(courtType);
        }
        if (request.getIsActive() != null) {
            existingCourt.setActive(request.getIsActive());
        }

        Court savedCourt = courtRepository.save(existingCourt);
        return toResponse(savedCourt);
    }


    private Court toEntity(CourtCreateRequest request) {
        Court court = new Court();
        court.setName(request.getName());

        CourtType courtType = findCourtTypeOrThrow(request.getCourtTypeId());
        court.setCourtType(courtType);

        return court;
    }

    private CourtResponse toResponse(Court court) {
        return new CourtResponse(court.getId(), court.getName(), court.getCourtType().getId(), court.isActive());
    }

    private Court findCourtOrThrow(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new CourtNotFoundException("Court not found with id: " + id));
    }

    private CourtType findCourtTypeOrThrow(Long id) {
        return courtTypeRepository.findById(id)
                .orElseThrow(() -> new CourtTypeNotFoundException("Court Type not found with id: " + id));
    }
}
