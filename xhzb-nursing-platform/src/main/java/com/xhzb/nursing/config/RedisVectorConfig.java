package com.xhzb.nursing.config;

import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * redis向量数据库配置类
 */
@Configuration
public class RedisVectorConfig {

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("115.159.84.87", 6378);
    }

    /**
     * 构建向量数据库操作对象
     * @param jedisPooled
     * @param openAiEmbeddingModel
     * @return
     */
    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, OpenAiEmbeddingModel openAiEmbeddingModel) {
        return RedisVectorStore.builder(jedisPooled, openAiEmbeddingModel)
                .indexName("spring-ai-index")                //可选：索引名称。 默认"spring-ai-index"
                .prefix("doc:")                  // 可选：前缀 默认："embedding:" 开头
//                .metadataFields(                         // 可选：定义元数据字段 可以用来做过滤
//                        RedisVectorStore.MetadataField.tag("country"),
//                        RedisVectorStore.MetadataField.numeric("year"))
                .initializeSchema(true)                   // 可选: 是否在项目启动时创建他的向量数据库 默认： to false （开发阶段建议是true，生产环境建议false）
                .batchingStrategy(new TokenCountBatchingStrategy()) //可选: 批量操作策略。  默认是的 TokenCountBatchingStrategy 按照token数量做批量插入
                .build();//构建
    }

}