package com.pixalportal.backend.event;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.pixalportal.backend.storage.StorageService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
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

  // POST request to get an upload URL (Guest facing, protected by Rate Limiting)
    @PostMapping("/{id}/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(
            @PathVariable UUID id, 
            @RequestBody Map<String, String> requestBody) {
        
        String fileName = requestBody.get("fileName");
        String contentType = requestBody.get("contentType");

        if (fileName == null || contentType == null) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }

        // 1. Verify event ownership/existence (matching your Next.js logic)
        java.util.Optional<Event> eventOptional = eventService.findById(id);
        
        if (eventOptional.isEmpty()) {
            return ResponseEntity.status(403).build(); // 403 Forbidden (Invalid Event)
        }

        // 2. Generate the URLs
        Map<String, String> urls = storageService.generatePresignedUploadUrl(id, fileName, contentType);
        
        return ResponseEntity.ok(urls);
    }

    // GET request to fetch all image URLs for a specific event
    @GetMapping("/{id}/images")
    public ResponseEntity<List<Map<String, String>>> getEventImages(
            @PathVariable UUID id, 
            @AuthenticationPrincipal Jwt jwt) {
            
        UUID userId = UUID.fromString(jwt.getSubject());
        
        // 1. Authorisation check: ensure the event exists and the user owns it
        return eventService.findById(id).map(event -> {
            if (!event.getCreatedByUserId().equals(userId)) {
                return ResponseEntity.status(403).<List<Map<String, String>>>build(); // Forbidden
            }
            
            // 2. If authorised, fetch the images
            List<Map<String, String>> images = storageService.listEventImages(id);
            return ResponseEntity.ok(images);
            
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE request to remove a specific image from an event
    @DeleteMapping("/{id}/images")
    public ResponseEntity<Void> deleteEventImage(
            @PathVariable UUID id, 
            @RequestParam String key, 
            @AuthenticationPrincipal Jwt jwt) {
            
        UUID userId = UUID.fromString(jwt.getSubject());
        
        // Ensure you have java.util.Optional imported at the top of your file
        java.util.Optional<Event> eventOptional = eventService.findById(id);
        
        if (eventOptional.isEmpty()) {
            return ResponseEntity.notFound().build(); // 404 if the event doesn't exist
        }
        
        Event event = eventOptional.get();
        
        // 1. Authorisation check: Only the authenticated owner may delete images
        if (!event.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build(); // 403 Forbidden
        }
        
        // 2. Belt-and-braces: Never let a request delete something outside the event's folder
        String expectedPrefix = "events/" + id.toString() + "/";
        if (!key.startsWith(expectedPrefix)) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }
        
        // 3. Execute the deletion
        try {
            storageService.deleteImage(key);
            return ResponseEntity.noContent().build(); // 204 No Content (Standard successful delete)
        } catch (Exception e) {
            // If AWS fails, return a 500 status
            return ResponseEntity.internalServerError().build(); 
        }
    }

    
}