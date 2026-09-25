package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommandConfigRepository
        extends JpaRepository<CommandConfig, Long> {

    Optional<CommandConfig> findByCommandName(String commandName);
}