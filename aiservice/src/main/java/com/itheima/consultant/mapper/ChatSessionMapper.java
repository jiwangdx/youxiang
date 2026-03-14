package com.itheima.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.consultant.pojo.ChatSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId}")
    ChatSession findBySessionId(String sessionId);

    @Insert("INSERT INTO chat_session(session_id, session_title, messages, created_at, updated_at, is_important, last_access_time) " +
            "VALUES(#{sessionId}, #{sessionTitle}, #{messages}, #{createdAt}, #{updatedAt}, #{isImportant}, #{lastAccessTime})")
    void insertSession(@Param("sessionId") String sessionId,
                      @Param("sessionTitle") String sessionTitle,
                      @Param("messages") String messages,
                      @Param("createdAt") LocalDateTime createdAt,
                      @Param("updatedAt") LocalDateTime updatedAt,
                      @Param("isImportant") Boolean isImportant,
                      @Param("lastAccessTime") LocalDateTime lastAccessTime);

    @Update("UPDATE chat_session SET messages = #{messages}, updated_at = #{updatedAt}, last_access_time = #{lastAccessTime} WHERE session_id = #{sessionId}")
    void updateBySessionId(@Param("sessionId") String sessionId,
                          @Param("messages") String messages,
                          @Param("updatedAt") LocalDateTime updatedAt,
                          @Param("lastAccessTime") LocalDateTime lastAccessTime);

    @Update("UPDATE chat_session SET is_important = #{isImportant}, last_access_time = NOW() WHERE session_id = #{sessionId}")
    void updateImportantFlag(String sessionId, Boolean isImportant);

    @Select("SELECT * FROM chat_session WHERE last_access_time < #{threshold} AND is_important = false ORDER BY last_access_time ASC LIMIT #{limit}")
    List<ChatSession> findExpiredNonImportant(LocalDateTime threshold, int limit);

    @Update("UPDATE chat_session SET last_access_time = NOW() WHERE id = #{id}")
    void updateLastAccessTime(Long id);
    
    @Update("UPDATE chat_session SET last_access_time = NOW() WHERE session_id = #{sessionId}")
    void updateLastAccessTimeBySessionId(String sessionId);

    @Select("SELECT * FROM chat_session ORDER BY last_access_time DESC")
    List<ChatSession> findAllSessions();
    
    @Select("SELECT messages FROM chat_session WHERE session_id = #{sessionId}")
    String findMessagesBySessionId(String sessionId);
    
    @Delete("DELETE FROM chat_session WHERE id = #{id}")
    void deleteById(Long id);
}
