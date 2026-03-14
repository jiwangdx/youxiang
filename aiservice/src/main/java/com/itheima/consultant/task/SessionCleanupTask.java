package com.itheima.consultant.task;

import com.itheima.consultant.service.SessionPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupTask {
    @Autowired
    private SessionPersistenceService sessionPersistenceService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredSessions() {
        sessionPersistenceService.cleanupExpiredSessions();
    }
}
