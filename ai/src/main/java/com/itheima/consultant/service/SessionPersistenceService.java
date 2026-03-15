package com.itheima.consultant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.consultant.mapper.ChatSessionMapper;
import com.itheima.consultant.pojo.ChatSession;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SessionPersistenceService {
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("===== 初始化数据库表 =====");
        try {
            String createTableSql = "CREATE TABLE IF NOT EXISTS chat_session (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "session_id VARCHAR(255) NOT NULL UNIQUE, " +
                    "session_title VARCHAR(500), " +
                    "messages TEXT NOT NULL, " +
                    "created_at DATETIME NOT NULL, " +
                    "updated_at DATETIME NOT NULL, " +
                    "is_important BOOLEAN DEFAULT FALSE, " +
                    "last_access_time DATETIME NOT NULL, " +
                    "INDEX idx_session_id (session_id), " +
                    "INDEX idx_last_access_time (last_access_time)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            jdbcTemplate.execute(createTableSql);
            System.out.println("===== 数据库表初始化完成 =====");
        } catch (Exception e) {
            System.err.println("初始化表时出错：" + e.getMessage());
        }
    }

    public void createInitialSession(String sessionId) {
        System.out.println("===== 创建初始会话: " + sessionId + " =====");
        List<ChatMessage> initialMessages = new ArrayList<>();
        initialMessages.add(UserMessage.from("你好"));
        initialMessages.add(AiMessage.from("你好！我是优享生活圈提供的AI顾问，请问有什么能帮到您？您也可以直接点击上方的服务卡片快速使用相关功能。"));
        
        persistSession(sessionId, initialMessages);
        System.out.println("===== 初始会话创建完成 =====");
    }

    public void persistSession(String sessionId, List<ChatMessage> messages) {
        System.out.println("===== 持久化会话: " + sessionId + " =====");
        String messagesJson = ChatMessageSerializer.messagesToJson(messages);
        
        // 先检查是否存在
        String checkSql = "SELECT id FROM chat_session WHERE session_id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, sessionId);
        
        if (results.size() > 0) {
            System.out.println("找到现有会话，更新中...");
            String updateSql = "UPDATE chat_session SET messages = ?, updated_at = NOW(), last_access_time = NOW() WHERE session_id = ?";
            jdbcTemplate.update(updateSql, messagesJson, sessionId);
            System.out.println("会话更新完成");
        } else {
            System.out.println("创建新会话...");
            String insertSql = "INSERT INTO chat_session(session_id, messages, created_at, updated_at, is_important, last_access_time) " +
                              "VALUES(?, ?, NOW(), NOW(), false, NOW())";
            jdbcTemplate.update(insertSql, sessionId, messagesJson);
            System.out.println("新会话插入完成");
        }
    }

    public List<ChatMessage> loadSession(String sessionId) {
        String sql = "SELECT messages FROM chat_session WHERE session_id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, sessionId);
        
        if (results.size() > 0) {
            String messagesJson = (String) results.get(0).get("messages");
            // 更新最后访问时间
            String updateSql = "UPDATE chat_session SET last_access_time = NOW() WHERE session_id = ?";
            jdbcTemplate.update(updateSql, sessionId);
            return ChatMessageDeserializer.messagesFromJson(messagesJson);
        }
        return new ArrayList<>();
    }

    public void markSessionAsImportant(String sessionId, boolean isImportant) {
        String sql = "UPDATE chat_session SET is_important = ?, last_access_time = NOW() WHERE session_id = ?";
        jdbcTemplate.update(sql, isImportant, sessionId);
    }

    public void cleanupExpiredSessions() {
        String sql = "DELETE FROM chat_session WHERE last_access_time < DATE_SUB(NOW(), INTERVAL 90 DAY) AND is_important = false LIMIT 1000";
        jdbcTemplate.update(sql);
    }

    public List<Map<String, Object>> getAllSessions() {
        String sql = "SELECT * FROM chat_session ORDER BY last_access_time DESC";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : results) {
            if (row.containsKey("session_id")) {
                row.put("sessionId", row.get("session_id"));
            }
            if (row.containsKey("session_title")) {
                row.put("sessionTitle", row.get("session_title"));
            }
            if (row.containsKey("created_at")) {
                row.put("createdAt", row.get("created_at"));
            }
            if (row.containsKey("updated_at")) {
                row.put("updatedAt", row.get("updated_at"));
            }
            if (row.containsKey("is_important")) {
                row.put("isImportant", row.get("is_important"));
            }
            if (row.containsKey("last_access_time")) {
                row.put("lastAccessTime", row.get("last_access_time"));
            }
        }
        
        return results;
    }

    public void deleteSession(String sessionId) {
        String sql = "DELETE FROM chat_session WHERE session_id = ?";
        jdbcTemplate.update(sql, sessionId);
    }
    
    public void updateSessionTitle(String sessionId, String title) {
        System.out.println("===== 更新会话标题: " + sessionId + " 新标题: " + title + " =====");
        String sql = "UPDATE chat_session SET session_title = ?, last_access_time = NOW() WHERE session_id = ?";
        jdbcTemplate.update(sql, title, sessionId);
        System.out.println("===== 会话标题更新完成 =====");
    }
    
    public String extractFirstUserMessage(List<ChatMessage> messages) {
        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage userMsg) {
                String text = userMsg.singleText();
                if (text != null && !text.isEmpty()) {
                    return text.length() > 30 ? text.substring(0, 30) + "..." : text;
                }
            }
        }
        return "新会话";
    }
    
    public void autoSetSessionTitle(String sessionId, List<ChatMessage> messages) {
        System.out.println("===== 自动设置会话标题: " + sessionId + " =====");
        String title = extractFirstUserMessage(messages);
        String sql = "UPDATE chat_session SET session_title = ?, last_access_time = NOW() WHERE session_id = ? AND session_title IS NULL";
        int updated = jdbcTemplate.update(sql, title, sessionId);
        if (updated > 0) {
            System.out.println("===== 会话标题已自动设置为: " + title + " =====");
        }
    }
}
