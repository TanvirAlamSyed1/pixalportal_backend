package com.pixalportal.backend.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    // This matches the field name 'createdByUserId' in your Event entity
    List<Event> findByCreatedByUserId(UUID createdByUserId);
}