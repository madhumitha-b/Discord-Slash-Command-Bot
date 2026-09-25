package com.example.project;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final CommandLogRepository commandLogRepository;

    public AdminDashboardController(CommandLogRepository commandLogRepository) {
        this.commandLogRepository = commandLogRepository;
    }

    @GetMapping("/logs")
    public List<CommandLog> getLogs() {
        return commandLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}