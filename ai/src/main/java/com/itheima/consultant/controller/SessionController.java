package com.itheima.consultant.controller;

import com.itheima.consultant.service.SessionPersistenceService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin
public class SessionController {

    @Autowired
    private SessionPersistenceService sessionPersistenceService;

    @PostMapping("/create")
    public void createInitialSession(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        sessionPersistenceService.createInitialSession(sessionId);
    }

    @GetMapping("/list")
    public List<Map<String, Object>> getAllSessions() {
        return sessionPersistenceService.getAllSessions();
    }

    @GetMapping("/load/{sessionId}")
    public String loadSession(@PathVariable String sessionId) {
        List<ChatMessage> messages = sessionPersistenceService.loadSession(sessionId);
        return ChatMessageSerializer.messagesToJson(messages);
    }

    @PostMapping("/mark-important")
    public void markSessionAsImportant(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        Boolean isImportant = (Boolean) request.get("isImportant");
        sessionPersistenceService.markSessionAsImportant(sessionId, isImportant);
    }

    @DeleteMapping("/delete/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        sessionPersistenceService.deleteSession(sessionId);
    }

    @PostMapping("/rename")
    public void renameSession(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        String title = (String) request.get("title");
        sessionPersistenceService.updateSessionTitle(sessionId, title);
    }
}
