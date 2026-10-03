package com.xhzb.nursing.config;

import com.xhzb.nursing.constants.SystemConstants;
import com.xhzb.nursing.service.impl.RedisChatMemoryService;
import com.xhzb.nursing.tool.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    /**
     * 声明chatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                 RedisChatMemoryService redisChatMemoryService,
                                 WeatherTools weatherTools,
                                 VectorStore vectorStore) {
        //将VectorStore对象包装成QuestionAnswerAdvisor
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.7d)//设置阈值
                        .topK(5)//前五个文档
                        .build())
                .build();
        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem(SystemConstants.prompt)
                //.defaultTools(weatherTools)
                .defaultAdvisors(new SimpleLoggerAdvisor(),//添加日志处理器（aop切面编程机制
                        MessageChatMemoryAdvisor.builder(redisChatMemoryService).build()//将我们写的RedisChatMemoryService包装成切面处理器
                        ,questionAnswerAdvisor)
                .build();

    }

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)  //目标块大小  token数
                .withMinChunkSizeChars(200) // 最小块的字符数
                .withMinChunkLengthToEmbed(10) // 最小的文本字符长度
                .withMaxNumChunks(10000)  //文档最大块数
                .withKeepSeparator(false)   //不保留换行符
                .build();
    }
}
