package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.service.SlotGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/slots")
@RequiredArgsConstructor
public class AdminSlotController {

    private final SlotGenerationService slotGenerationService;

    /** Manual trigger, e.g. right after creating a new court. ADMIN only (see SecurityConfig). */
    @PostMapping("/generate")
    public Map<String, Integer> generate() {
        return Map.of("created", slotGenerationService.generateSlots());
    }
}
