package com.xhzb.nursing.controller;


import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.service.ChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {
    //注入chatClient
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 对话接口
     * @param prompt 提示词
     * @param chatId 会话id 前端采用时间戳生成的一个数字
     * @return
     */
    @PostMapping("/chat")
    public Flux<String> chat(String prompt, String chatId){
        //将用户id和会话id记录到redis
        chatHistoryService.save(SecurityUtils.getUserId()+"",chatId);
        //调用chatclient和达模型对话
        return chatClient
                .prompt()
                .user(prompt)//将用户的提示词作为user提示词
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, chatId))//将chatid传到Springai上下文中
                .stream()//采用流式
                .content();//拿到达模型返回的内容

    }
   /* @PostMapping("/chat")
    public String chat(String prompt,String chatId){
        //调用chatclient和达模型对话
        return chatClient
                .prompt()
                .user(prompt)//将用户的提示词作为user提示词
                .call()//调用大模型接口
                .content();//拿到达模型返回的内容

    }*/

}
