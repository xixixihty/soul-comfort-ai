package com.hxq.soulcomfortai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SoulConformTools {

    @Tool(name = "StoryQuerySearch", value = "查询治愈心灵的故事，你可以从这个网站上查询：https://www.ruiwen.com")
    public String searchStory(@P(value = "根据这个关键词来查询", required = true) String keyword) {
        long start = System.currentTimeMillis();
        String encodeKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.ruiwen.com/search?q=" + encodeKeyword;

        try {
            Document doc = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return Jsoup.connect(url)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    .timeout(5000)
                                    .get();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .get(3, TimeUnit.SECONDS);

            Elements elements = doc.select(".story-list .story-item");
            String result = elements.stream()
                    .map(org.jsoup.nodes.Element::text)
                    .collect(Collectors.joining("\n"));

            if (result.isEmpty()) {
                log.warn("[StoryQuerySearch] keyword={} | no results | {}ms", keyword, System.currentTimeMillis() - start);
                return "未找到相关内容";
            }

            log.info("[StoryQuerySearch] keyword={} | resultCount={} | {}ms", keyword, elements.size(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[StoryQuerySearch] keyword={} | failed: {} | {}ms", keyword, e.getMessage(), System.currentTimeMillis() - start);
            return "查询超时，请稍后重试";
        }
    }
}
