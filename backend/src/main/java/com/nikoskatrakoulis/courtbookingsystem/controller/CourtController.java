package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtCreateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.request.CourtUpdateRequest;
import com.nikoskatrakoulis.courtbookingsystem.dto.response.CourtResponse;
import com.nikoskatrakoulis.courtbookingsystem.service.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @GetMapping
    public List<CourtResponse> getAll() {
        return courtService.getAllCourts();
    }

    @GetMapping("/{id}")
    public CourtResponse getCourt(@PathVariable(name = "id") Long id) {
        return courtService.getCourtById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourtResponse createCourt(@Valid @RequestBody CourtCreateRequest request) {
        return courtService.createCourt(request);
    }

    @PatchMapping("/{id}")
    public CourtResponse updateCourt(@PathVariable(name = "id") Long id, @Valid @RequestBody CourtUpdateRequest request) {
        return courtService.updateCourtById(id, request);
    }
}
