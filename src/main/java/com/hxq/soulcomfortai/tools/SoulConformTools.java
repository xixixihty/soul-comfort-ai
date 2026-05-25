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

@Slf4j

public class SoulConformTools {


    @Tool(name = "StoryQuerySearch" , value = "查询治愈心灵的故事，你可以从这个网站上查询：https://www.ruiwen.com")
    public String searchStory(@P(value = "根据这个关键词来查询", required = true) String keyword) {
        String encodeKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.ruiwen.com/search?q=" + encodeKeyword;
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();
        } catch (IOException e) {
            log.error("查询失败", e);
            return "查询失败";
        }
        Elements elements = doc.select(".story-list .story-item");
        StringBuilder sb = new StringBuilder();
        return String.join("\n", elements.stream().map(element -> element.text()).toArray(String[]::new));
    }

}
