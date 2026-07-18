package com.pixalportal.backend.event;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.pixalportal.backend.storage.StorageService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final StorageService storageService; // 1. Declare the field

    // 2. Add StorageService to the constructor for Dependency Injection
    public EventController(EventService eventService, StorageService storageService) {
        this.eventService = eventService;
        this.storageService = storageService;
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

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        return eventService.findById(id) // The 'id' must be passed here
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT request to edit an existing event
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable UUID id, 
            @RequestBody Event updatedEvent, 
            @AuthenticationPrincipal Jwt jwt) {
            
        UUID userId = UUID.fromString(jwt.getSubject());
        
        // Pass userId to the service layer to ensure the user editing the event is the actual creator
        return eventService.updateEvent(id, updatedEvent, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE request to remove an event
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id, 
            @AuthenticationPrincipal Jwt jwt) {
            
        UUID userId = UUID.fromString(jwt.getSubject());
        
        // Pass userId to the service to verify ownership before deletion
        boolean isDeleted = eventService.deleteEvent(id, userId);
        
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build(); // Or return 403 Forbidden if they don't own it
        }
    }

   @PostMapping("/{id}/upload-url")
    public ResponseEntity<String> getUploadUrl(
            @PathVariable UUID id, 
            @RequestParam String fileName) {
        
        // 1. Validate the event exists
        return eventService.findById(id)
                .map(event -> {
                    // 2. Generate the URL only if the event exists
                    String url = storageService.generatePresignedUploadUrl(id, fileName);
                    return ResponseEntity.ok(url);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}