package com.itheima.consultant.controller;

import com.itheima.consultant.aiservice.ConsultantService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    @Autowired
    private ConsultantService consultantService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message) {
        Flux<String> result = consultantService.chat(memoryId, message);
        return result;
    }


}

//    接收请求：后端接收 HTTP 请求并分配一个线程。
//    AI 工作开始：AI 开始生成响应，线程被释放去处理其他任务。
//    数据生成与推送：AI 持续生成数据并通过流的方式分块推送给前端。
//    通知与恢复：每当新数据准备好，线程会恢复工作，推送数据到前端，直到任务完成。

//    ChatController 只是做一个简单的路由转发，将请求传递给 ConsultantService 层进行实际处理。