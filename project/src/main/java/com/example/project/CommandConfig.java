package com.example.project;

import jakarta.persistence.*;

@Entity
@Table(name = "command_configs")
public class CommandConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String commandName;

    private boolean enabled;

    @Column(length = 1000)
    private String responseMessage;

    private boolean mirrorEnabled;

    public Long getId() {
        return id;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public boolean isMirrorEnabled() {
        return mirrorEnabled;
    }

    public void setMirrorEnabled(boolean mirrorEnabled) {
        this.mirrorEnabled = mirrorEnabled;
    }
}