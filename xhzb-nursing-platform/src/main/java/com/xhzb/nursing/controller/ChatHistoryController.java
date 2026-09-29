package com.xhzb.nursing.controller;

import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.vo.MessageVO;
import com.xhzb.nursing.service.ChatHistoryService;
import com.xhzb.nursing.service.impl.RedisChatMemoryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChatHistoryController {

    /**
     * 根据用户id获取会话id集合
     */
    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private RedisChatMemoryService redisChatMemoryService;

    @GetMapping("ai/history")
    public AjaxResult getChatIds(){
        Long userId = SecurityUtils.getUserId();
        List<String> chatIds = chatHistoryService.getChatIds(userId.toString());
        return AjaxResult.success(chatIds);
    }

    /**
     * 根据会话id获取会话记录详情
     */
    @GetMapping("ai/history/{chatId}")
    public AjaxResult getChatId(@PathVariable String chatId){
        //从redis中获取详情
        List<Message> messages = redisChatMemoryService.get(chatId);
        //转换为MessageVo对象
        if (messages == null || messages.isEmpty()) {
            return AjaxResult.success();
        }

        List<MessageVO> list = messages.stream().map(message -> {
            return new MessageVO(message);
        }).toList();
        return AjaxResult.success(list);
    }
    /**
     * 删除会话
     */
    @DeleteMapping("ai/history/{chatId}")
    public AjaxResult deleteChatId(@PathVariable String chatId){
        //删除redis中当前用户的会话集合中的。 会话id    set中移除某个元素
        chatHistoryService.deleteHistory(chatId);

        //删除会话详情
        redisChatMemoryService.clear(chatId);
        return AjaxResult.success();
    }

}
