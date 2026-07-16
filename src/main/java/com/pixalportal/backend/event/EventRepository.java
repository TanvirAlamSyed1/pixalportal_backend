package com.pixalportal.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    // Spring Boot automatically translates this method name into a SQL query:
    // SELECT * FROM "Event" WHERE "CreatedByUserID" = ?
    List<Event> findByCreatedByUserId(UUID createdByUserId);
}