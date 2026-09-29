package com.xhzb.nursing.service.impl;

import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {


    @Autowired
    private RedisTemplate<String, String>  redisTemplate;
    private static final String CHAT_HISTORY_PREFIX = "chat:history:";

    @Override
    public void save(String userId, String chatId) {
        redisTemplate.opsForSet().add(CHAT_HISTORY_PREFIX + userId, chatId);
    }

    @Override
    public List<String> getChatIds(String userId) {

        Set<String> chatIds = redisTemplate.opsForSet().members(CHAT_HISTORY_PREFIX + userId);
        if (chatIds == null || chatIds.isEmpty()) {
            return List.of();
        }
        return chatIds.stream().sorted(Comparator.comparing(String::toString).reversed()).toList();//降序
        //return chatIds.stream().sorted().toList();

    }
    @Override
    public void deleteHistory(String chatId) {
        Long userId = SecurityUtils.getUserId();
        redisTemplate.opsForSet().remove(CHAT_HISTORY_PREFIX + userId, chatId);
    }
}









