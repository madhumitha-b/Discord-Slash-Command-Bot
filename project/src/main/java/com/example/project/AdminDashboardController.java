package com.example.project;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final CommandLogRepository commandLogRepository;
    private final CommandConfigRepository commandConfigRepository;

    public AdminDashboardController(
            CommandLogRepository commandLogRepository,
            CommandConfigRepository commandConfigRepository) {

        this.commandLogRepository = commandLogRepository;
        this.commandConfigRepository = commandConfigRepository;
    }

    // Get command logs
    @GetMapping("/logs")
    public List<CommandLog> getLogs() {

        return commandLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // Get command configurations
    @GetMapping("/configs")
    public List<CommandConfig> getConfigs() {

        return commandConfigRepository.findAll(
                Sort.by("commandName")
        );
    }

    // Update command configuration
    @PutMapping("/configs/{commandName}")
    public CommandConfig updateConfig(
            @PathVariable String commandName,
            @RequestBody CommandConfig request) {

        CommandConfig config = commandConfigRepository
                .findByCommandName(commandName)
                .orElseThrow(() ->
                        new RuntimeException("Command not found")
                );

        config.setEnabled(request.isEnabled());
        config.setResponseMessage(request.getResponseMessage());
        config.setMirrorEnabled(request.isMirrorEnabled());

        return commandConfigRepository.save(config);
    }
}