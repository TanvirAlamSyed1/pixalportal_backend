package com.pixalportal.backend.event;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"Event\"") // Escaped quotes force PostgreSQL to look for the capital 'E'
public class Event {

   @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "\"EventID\"", updatable = false, nullable = false)
    private UUID eventId;

    @Column(name = "\"CreatedByUserID\"", nullable = false)
    private UUID createdByUserId; // Use camelCase here

    @Column(name = "\"Name\"", nullable = false)
    private String name;

    @Column(name = "\"StartDate\"")
    private LocalDateTime startDate; // Or OffsetDateTime if time zone precision is critical

    @Column(name = "\"EndDate\"")
    private LocalDateTime endDate;

    @Column(name = "\"Address\"")
    private String address;

    @Column(name = "\"Postcode\"")
    private String postcode;

    @Column(name = "\"MapURL\"")
    private String mapURL;

    @Column(name = "\"Description\"")
    private String description;
    
    @Column(name = "\"S3FolderCreated\"")
    private Boolean s3FolderCreated = false;

    // Standard no-argument constructor required by JPA
    public Event() {}

    // --- Getters and Setters ---
    
    public UUID getEventId() { 
        return eventId; 
    }
    
    public void setEventId(UUID eventId) { 
        this.eventId = eventId; 
    }

   public UUID getCreatedByUserId() {
    return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public LocalDateTime getStartDate() { 
        return startDate; 
    }
    
    public void setStartDate(LocalDateTime startDate) { 
        this.startDate = startDate; 
    }

    public LocalDateTime getEndDate() { 
        return endDate; 
    }
    
    public void setEndDate(LocalDateTime endDate) { 
        this.endDate = endDate; 
    }

    public String getAddress() { 
        return address; 
    }
    
    public void setAddress(String address) { 
        this.address = address; 
    }

    public String getPostcode() { 
        return postcode; 
    }
    
    public void setPostcode(String postcode) { 
        this.postcode = postcode; 
    }

    public String getMapUrl() { 
        return mapURL; 
    }
    
    public void setMapUrl(String mapUrl) { 
        this.mapURL = mapUrl; 
    }

    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }

    public Boolean getS3FolderCreated() { 
        return s3FolderCreated; 
    }
    
    public void setS3FolderCreated(Boolean s3FolderCreated) { 
        this.s3FolderCreated = s3FolderCreated; 
    }
}