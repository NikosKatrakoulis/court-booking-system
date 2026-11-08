package com.nikoskatrakoulis.courtbookingsystem.controller;

import com.nikoskatrakoulis.courtbookingsystem.dto.response.WaitlistEntryResponse;
import com.nikoskatrakoulis.courtbookingsystem.entity.User;
import com.nikoskatrakoulis.courtbookingsystem.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/slots/{slotId}/waitlist")
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistEntryResponse join(@AuthenticationPrincipal User user, @PathVariable Long slotId) {
        return waitlistService.join(user.getId(), slotId);
    }

    @GetMapping("/waitlist")
    public List<WaitlistEntryResponse> myEntries(@AuthenticationPrincipal User user) {
        return waitlistService.myEntries(user.getId());
    }

    @DeleteMapping("/waitlist/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal User user, @PathVariable Long id) {
        waitlistService.leave(user.getId(), id);
    }
}
