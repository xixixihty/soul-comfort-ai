package com.hxq.soulcomfortai.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SoulComfortServiceTest {
    @Resource
    private SoulComfortService soulComfortService;





    @Test
    void chat() {
        String message = "你好啊！我有一点难受，可以给我一些心灵安慰吗？";
        System.out.println(soulComfortService.chat(message));
    }

    @Test
    void chatWithMessage() {
        String result = soulComfortService.chat("我正在被 anxiety 情绪所困扰，请给我一些解决方案");
        System.out.println(result);
        result = soulComfortService.chat("我的情绪是什么？");
        System.out.println(result);
    }

    @Test
    void chatForReport() {
        String message = "我正在被 anxiety 情绪所困扰，请给我一些解决方案";
        System.out.println(soulComfortService.chatForReport(message));
    }
    @Test
    void chatWithRag() {
        String result = soulComfortService.chat("我的感觉心情烦躁、大脑紧绷、胡思乱想，你有哪一些建议可以缓解？");
        System.out.println(result);
    }
}