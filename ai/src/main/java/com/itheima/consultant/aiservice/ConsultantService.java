package com.itheima.consultant.aiservice;

import com.itheima.consultant.tools.ReservationTool;
import com.itheima.consultant.tools.ShopTool;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,//手动装配
        chatModel = "openAiChatModel",//指定模型
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",//配置会话记忆提供者对象
        contentRetriever = "contentRetriever",//配置向量数据库检索对象
        tools = {"shopTool","reservationTool","voucherTool"}
)
//@AiService
public interface ConsultantService {
    @SystemMessage(fromResource = "system.txt")

    public Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
