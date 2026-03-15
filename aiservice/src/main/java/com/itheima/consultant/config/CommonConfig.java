package com.itheima.consultant.config;



import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class CommonConfig {
//    @Autowired 会根据类型来查找对应的 Bean。
//    如果存在多个 ChatMemoryStore 实现类（比如 RedisChatMemoryStore 和其他存储实现），
//    Spring 会根据需要选择合适的 Bean。如果只有一个实现类（如 RedisChatMemoryStore），Spring 会自动注入它。
    @Autowired
    private OpenAiChatModel model;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private EmbeddingModel embeddingModel;

//    //构建会话记忆对象
//    @Bean
//    public ChatMemory chatMemory(){
//        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
//                .maxMessages(20)
//                .build();
//        return memory;
//    }

    //构建ChatMemoryProvider对象

    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        System.out.println("构建一次");
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build();
            }
        };
        return chatMemoryProvider;
    }






//    @Bean
//    public EmbeddingStore store() {
//        // 1. 加载文档，使用 Apache PDFBox 解析 PDF 文件
//        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content", new ApachePdfBoxDocumentParser());
//
//        // 2. 构建向量数据库操作对象（操作的是内存版本的向量数据库）
//        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
//
//        // 3. 构建文档分割器对象，递归分割
//        DocumentSplitter ds = DocumentSplitters.recursive(500, 100);
//
//        // 4. 构建一个 EmbeddingStoreIngestor 对象，完成文本数据切割，向量化，存储
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .embeddingStore(store)
//                .documentSplitter(ds)
//                .embeddingModel(embeddingModel)
//                .build();
//
//        // 5. 向量化并存储文档
//        ingestor.ingest(documents);
//
//        return store;
//    }

    @Value("${langchain4j.community.chroma.host:localhost}")
    private String chromaHost;

    @Value("${langchain4j.community.chroma.port:8000}")
    private int chromaPort;

    @Bean
    public EmbeddingStore store() {
        ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                .baseUrl("http://" + chromaHost + ":" + chromaPort)
                .collectionName("shop_embeddings")
                .build();
        return store;
    }







    //构建向量数据库检索对象
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore store){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
