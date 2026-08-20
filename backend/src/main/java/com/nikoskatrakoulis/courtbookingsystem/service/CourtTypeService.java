package com.nikoskatrakoulis.courtbookingsystem.service;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtTypeCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtTypeUpdateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtTypeResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.CourtType;
import com.nikoskatrakoulis.courtbookingsystem.exception.CourtTypeNotFoundException;
import com.nikoskatrakoulis.courtbookingsystem.repository.CourtTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtTypeService {

    private final CourtTypeRepository courtTypeRepository;

    public List<CourtTypeResponse> getAllCourtTypes() {
        List<CourtType> courtTypes = courtTypeRepository.findAll();
        return courtTypes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CourtTypeResponse getCourtTypeById(Long id) {
        CourtType courtType = findCourtTypeOrThrow(id);
        return toResponse(courtType);
    }

    public CourtTypeResponse createCourtType(CourtTypeCreateRequest request) {
        CourtType courtType = toEntity(request);
        CourtType savedCourtType = courtTypeRepository.save(courtType);
        return toResponse(savedCourtType);
    }

    public CourtTypeResponse updateCourtType(Long id, CourtTypeUpdateRequest request) {
        CourtType existingCourtType = findCourtTypeOrThrow(id);

        if (request.getName() != null) {
            existingCourtType.setName(request.getName());
        }
        if (request.getDurationMinutes() != null) {
            existingCourtType.setDurationMinutes(request.getDurationMinutes());
        }

        if (request.getPricePerSlot() != null) {
            existingCourtType.setPricePerSlot(request.getPricePerSlot());
        }

        if (request.getIsActive() != null) {
            existingCourtType.setActive(request.getIsActive());
        }

        CourtType savedCourtType = courtTypeRepository.save(existingCourtType);
        return toResponse(savedCourtType);
    }

    private CourtType toEntity(CourtTypeCreateRequest request) {
        CourtType courtType = new CourtType();

        courtType.setName(request.getName());
        courtType.setDurationMinutes(request.getDurationMinutes());
        courtType.setPricePerSlot(request.getPricePerSlot());

        return courtType;
    }

    private CourtTypeResponse toResponse(CourtType courtType) {
        return new CourtTypeResponse(courtType.getId(), courtType.getName(), courtType.getDurationMinutes(), courtType.getPricePerSlot(), courtType.isActive());
    }

    private CourtType findCourtTypeOrThrow(Long id) {
        return courtTypeRepository.findById(id)
                .orElseThrow(()-> new CourtTypeNotFoundException("Court Type not found with id: " + id));
    }
}
