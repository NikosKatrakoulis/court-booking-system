package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtTypeCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtTypeUpdateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtTypeResponse;
import com.nikoskatrakoulis.courtbookingsystem.service.CourtTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/court_types")
@RequiredArgsConstructor
public class CourtTypeController {

    private final CourtTypeService courtTypeService;

    @GetMapping
    public List<CourtTypeResponse> getAll() {
        return courtTypeService.getAllCourtTypes();
    }

    @GetMapping("/{id}")
    public CourtTypeResponse getCourtType(@PathVariable(name = "id")Long id) {
        return courtTypeService.getCourtTypeById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourtTypeResponse createCourtType(@Valid @RequestBody CourtTypeCreateRequest request) {
        return courtTypeService.createCourtType(request);
    }

    @PatchMapping("/{id}")
    public CourtTypeResponse updateCourtType(@PathVariable(name = "id")Long id, @Valid @RequestBody CourtTypeUpdateRequest request) {
        return courtTypeService.updateCourtType(id, request);
    }
}
