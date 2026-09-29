package com.xhzb.nursing.service.impl;

import cn.hutool.json.JSONUtil;
import com.xhzb.nursing.domain.vo.Msg;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//基于redis存储会话记录
@Component
public class RedisChatMemoryService implements ChatMemory {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    //前缀。
    private static final String PREFIX = "chat:memory:";

    /**
     * 将消息添加到redis（用户向大模型法请求和大模型回复时候=调用
     * @param conversationId 会话id
     * @param messages spring ai 提供的message对象 转换为Msg对象
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        //校验
        if(messages==null||messages.isEmpty()){
            return;
        }
        ArrayList<String> list = new ArrayList<>();//Msg对象的json格式字符串。集合
        for (Message message : messages) {
            Msg msg = new Msg(message);
            list.add(JSONUtil.toJsonStr(msg));//{"messageType":xxx,"test":xxx,"matadata"xxx}
        }
        redisTemplate.opsForList().leftPushAll(PREFIX+conversationId, list);

    }

    /**
     * 从redis中获取会话消息
     * @param conversationId 会话id
     * @return
     */
    @Override
    public List<Message> get(String conversationId) {
        String key = PREFIX+conversationId;
        List<String> redisMsgStrList = redisTemplate.opsForList().range(key, 0, -1);
        if (redisMsgStrList == null || redisMsgStrList.isEmpty()) {
            return List.of();
        }
        //将redisMsgStrList转Msg对象集合 按时间顺序排序 转springai message对象集合
        List<Message> messagesList = redisMsgStrList.stream()
                .map(redisMsgStr -> JSONUtil.toBean(redisMsgStr, Msg.class))
                .sorted(Comparator.comparing(Msg::getCreateTime))//按照createTime做升序
                .map(Msg::toMessage)
                .toList();
        return  messagesList;

    }

    /**
     * 删除redis中会话记录
     * @param conversationId
     */
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(PREFIX+conversationId);

    }
}
