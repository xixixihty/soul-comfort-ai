package com.hxq.soulcomfortai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RAG 知识库配置：
 * <p>
 * 1. contentRetriever Bean 只做装配，不在此处调用外部 embedding API，应用启动不再依赖外部服务；
 * 2. 文档向量化改为应用就绪后异步执行，失败只记日志，不影响启动，RAG 暂时返回空结果。
 */
@Slf4j
@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel siliconflowEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {
        // 仅构建检索器，不在 Bean 创建阶段调用 embedding API（断网/欠费/无资源包均可正常启动）
        ContentRetriever delegate = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(siliconflowEmbeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
        // 容错降级：底层 embedding 调用失败时返回空结果，聊天功能不受影响
        return new TolerantContentRetriever(delegate);
    }

    /**
     * 应用启动完成后，异步将 docs 目录下的文档摄入向量库。
     * 摄入失败（断网/欠费/无资源包等）只记录告警日志，不影响应用可用性。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ingestDocumentsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .documentSplitter(new DocumentByParagraphSplitter(1000, 200))
                        .embeddingModel(siliconflowEmbeddingModel)
                        .embeddingStore(embeddingStore)
                        .build();
                ingestor.ingest(documents);
                log.info("RAG 文档摄入完成，共 {} 个文档", documents.size());
            } catch (Exception e) {
                log.warn("RAG 文档摄入失败（不影响应用启动），原因: {}", e.getMessage());
            }
        });
    }
}