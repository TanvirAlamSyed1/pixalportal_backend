package com.pixalportal.backend.event;

import org.springframework.stereotype.Service;
import com.pixalportal.backend.storage.StorageService;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final StorageService storageService;

    // Inject both the repository and our new storage service
    public EventService(EventRepository eventRepository, StorageService storageService) {
        this.eventRepository = eventRepository;
        this.storageService = storageService;
    }

    public Event createEvent(Event event, UUID userId) {
        // 1. Assign the user and save the initial event to get its generated UUID
        event.setCreatedByUserId(userId);
        Event savedEvent = eventRepository.save(event);

        try {
            // 2. Attempt to create the corresponding folder in AWS S3
            storageService.createEventFolder(savedEvent.getEventId());
            
            // 3. If successful, update the flag and save again
            savedEvent.setS3FolderCreated(true);
            return eventRepository.save(savedEvent);
            
        } catch (Exception e) {
            // If AWS fails (e.g. network issue), we log it, but the event still exists in the database.
            // In a production app, you might want to throw a custom exception or implement a retry mechanism here.
            System.err.println("Failed to create S3 folder for event: " + savedEvent.getEventId());
            e.printStackTrace();
            
            return savedEvent; // Returns the event with s3FolderCreated = false
        }
    }

    public List<Event> getUserEvents(UUID userId) {
        return eventRepository.findByCreatedByUserId(userId);
    }
}