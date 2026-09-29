package com.xhzb.nursing.service;

import java.util.List;

public interface ChatHistoryService {
    /**
     * 保存会话记录
     * @param userId 当前登录用户id
     * @param chatId 会话id
     */
    public void save(String userId, String chatId);

    /**
     * 根据用户id获取会话id集合
     */
    public List<String> getChatIds(String userId);

    /**
     * 删除会话id
     * @param chatId
     */
    void deleteHistory(String chatId);
}
