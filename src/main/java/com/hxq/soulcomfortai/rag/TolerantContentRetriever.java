package com.hxq.soulcomfortai.rag;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RAG 检索容错包装器：底层检索（如 embedding 向量化）异常时降级返回空结果。
 * <p>
 * 保证外部服务不可用时（欠费 / 断网 / Key 异常等）对话功能照常可用，知识库暂时不生效。
 */
@Slf4j
@RequiredArgsConstructor
public class TolerantContentRetriever implements ContentRetriever {

    private final ContentRetriever delegate;

    @Override
    public List<Content> retrieve(Query query) {
        try {
            return delegate.retrieve(query);
        } catch (Exception e) {
            log.warn("RAG 检索失败，已降级为纯对话模式，原因: {}", e.getMessage());
            return List.of();
        }
    }
}