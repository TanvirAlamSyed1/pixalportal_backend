package com.pixalportal.backend.event;

import org.springframework.stereotype.Service;
import com.pixalportal.backend.storage.StorageService;

import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    // ...

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

    // GET request support
    public Optional<Event> findById(UUID id) {
        return eventRepository.findById(id);
    }

    // PUT request support
    public Optional<Event> updateEvent(UUID id, Event updatedEvent, UUID userId) {
        return eventRepository.findById(id).map(existingEvent -> {
            // Authorisation check: ensure the user actually owns this event
            if (!existingEvent.getCreatedByUserId().equals(userId)) {
                throw new SecurityException("Unauthorised modification attempt."); 
            }
            
            // Update the permissible fields (do not overwrite IDs or S3 flags)
            existingEvent.setName(updatedEvent.getName());
            existingEvent.setStartDate(updatedEvent.getStartDate());
            existingEvent.setEndDate(updatedEvent.getEndDate());
            existingEvent.setAddress(updatedEvent.getAddress());
            existingEvent.setPostcode(updatedEvent.getPostcode());
            existingEvent.setMapUrl(updatedEvent.getMapUrl());
            existingEvent.setDescription(updatedEvent.getDescription());
            
            return eventRepository.save(existingEvent);
        });
    }

    @Transactional
    public boolean deleteEvent(UUID id, UUID userId) {
        return eventRepository.findById(id).map(existingEvent -> {
            // Authorisation check
            if (!existingEvent.getCreatedByUserId().equals(userId)) {
                logger.warn("Unauthorised deletion attempt for event ID: {} by user: {}", id, userId);
                throw new SecurityException("Unauthorised deletion attempt.");
            }

            // Attempt to clean up associated S3 resources
            try {
                storageService.deleteEventFolder(existingEvent.getEventId());
                logger.info("Successfully deleted S3 folder for event: {}", id);
            } catch (Exception e) {
                // Log failure but proceed with database deletion to avoid blocking the removal of the record
                logger.error("Failed to delete S3 folder for event: {}. Manual cleanup may be required.", id, e);
            }

            eventRepository.delete(existingEvent);
            logger.info("Successfully deleted event record: {}", id);
            return true;
        }).orElse(false);
    }

    
}