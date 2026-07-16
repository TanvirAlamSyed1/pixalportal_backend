package com.pixalportal.backend.event;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // POST request to create a new event
    @PostMapping
    public Event createEvent(@RequestBody Event event, @AuthenticationPrincipal Jwt jwt) {
        // Securely extract the user's UUID from the Supabase JWT token's 'sub' (subject) claim
        UUID userId = UUID.fromString(jwt.getSubject());
        
        return eventService.createEvent(event, userId);
    }

    // GET request to fetch the user's dashboard events
    @GetMapping
    public List<Event> getMyEvents(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        
        return eventService.getUserEvents(userId);
    }
}