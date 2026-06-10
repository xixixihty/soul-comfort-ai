package com.hxq.soulcomfortai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateLoader.class);

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadTemplate("daily_greeting", "prompt/daily_greeting_prompt.txt");
        loadTemplate("suggested_questions", "prompt/suggested_questions_prompt.txt");
        loadTemplate("diary_resonance", "prompt/diary_resonance_prompt.txt");
        log.info("Prompt 模板加载完成，共 {} 个模板", templateCache.size());
    }

    private void loadTemplate(String name, String classPath) {
        try {
            ClassPathResource resource = new ClassPathResource(classPath);
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            templateCache.put(name, content);
            log.info("Prompt 模板 [{}] 加载成功", name);
        } catch (IOException e) {
            log.error("Prompt 模板 [{}] 加载失败: {}", name, e.getMessage());
            throw new RuntimeException("Prompt 模板 [" + name + "] 加载失败，无法启动应用", e);
        }
    }

    public String render(String templateName, Map<String, String> params) {
        String template = templateCache.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Prompt 模板不存在: " + templateName);
        }
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}