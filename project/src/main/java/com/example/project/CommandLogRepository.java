package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    boolean existsByInteractionId(String interactionId);
}