package com.itheima.consultant.repository;

import com.itheima.consultant.service.SessionPersistenceService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SessionPersistenceService sessionPersistenceService;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        String json = redisTemplate.opsForValue().get(sessionId);
        if (json != null) {
            return ChatMessageDeserializer.messagesFromJson(json);
        }
        return sessionPersistenceService.loadSession(sessionId);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String sessionId = memoryId.toString();
        String json = ChatMessageSerializer.messagesToJson(list);
        redisTemplate.opsForValue().set(sessionId, json, Duration.ofDays(7));
        
        CompletableFuture.runAsync(() -> {
            sessionPersistenceService.persistSession(sessionId, list);
            sessionPersistenceService.autoSetSessionTitle(sessionId, list);
        });
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        redisTemplate.delete(sessionId);
    }
}
