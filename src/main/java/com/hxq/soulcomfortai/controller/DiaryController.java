package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.config.PromptTemplateLoader;
import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.dto.request.CreateDiaryRequest;
import com.hxq.soulcomfortai.dto.request.UpdateDiaryRequest;
import com.hxq.soulcomfortai.dto.response.DiaryVO;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.entity.Diary;
import com.hxq.soulcomfortai.repository.DiaryRepository;
import com.hxq.soulcomfortai.service.DiaryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryRepository diaryRepository;
    private final SoulComfortService soulComfortService;
    private final PromptTemplateLoader promptTemplateLoader;

    public DiaryController(DiaryService diaryService,
                           DiaryRepository diaryRepository,
                           SoulComfortService soulComfortService,
                           PromptTemplateLoader promptTemplateLoader) {
        this.diaryService = diaryService;
        this.diaryRepository = diaryRepository;
        this.soulComfortService = soulComfortService;
        this.promptTemplateLoader = promptTemplateLoader;
    }

    @PostMapping
    public ApiResponse<DiaryVO> create(@RequestAttribute("userId") String userId,
                                       @RequestBody CreateDiaryRequest request) {
        DiaryVO vo = diaryService.create(userId,
                request.getTitle(), request.getContent(), request.getMood());
        return ApiResponse.success(vo);
    }

    @GetMapping("/{id}")
    public ApiResponse<DiaryVO> getById(@RequestAttribute("userId") String userId,
                                        @PathVariable String id) {
        DiaryVO vo = diaryService.findById(id, userId);
        return ApiResponse.success(vo);
    }

    @PutMapping("/{id}")
    public ApiResponse<DiaryVO> update(@RequestAttribute("userId") String userId,
                                       @PathVariable String id,
                                       @RequestBody UpdateDiaryRequest request) {
        DiaryVO vo = diaryService.update(id, userId,
                request.getTitle(), request.getContent(), request.getMood());
        return ApiResponse.success(vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestAttribute("userId") String userId,
                                    @PathVariable String id) {
        diaryService.delete(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<DiaryVO>> list(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<DiaryVO> result = diaryService.list(userId, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/{diaryId}/resonance")
    public ApiResponse<Map<String, Object>> generateResonance(
            @RequestAttribute("userId") String userId,
            @PathVariable String diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("日记不存在或已过期"));
        if (!diary.getUserId().equals(userId)) {
            throw new com.hxq.soulcomfortai.exception.BusinessException(403, "无权操作该日记");
        }

        String prompt = promptTemplateLoader.render("diary_resonance",
                Map.of("diary_content", diary.getContent(),
                       "diary_mood", diary.getMood()));

        String resonance = soulComfortService.chatForReport(prompt);
        long now = System.currentTimeMillis();

        diary.setAiResonance(resonance);
        diary.setResonanceAt(now);
        diaryRepository.update(diary);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resonance", resonance);
        data.put("generatedAt", now);
        return ApiResponse.success(data);
    }

    @GetMapping("/{diaryId}/resonance")
    public ApiResponse<Map<String, Object>> getResonance(@RequestAttribute("userId") String userId,
                                                         @PathVariable String diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("日记不存在或已过期"));
        if (!diary.getUserId().equals(userId)) {
            throw new com.hxq.soulcomfortai.exception.BusinessException(403, "无权访问该日记");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        if (diary.getAiResonance() != null) {
            data.put("resonance", diary.getAiResonance());
            data.put("generatedAt", diary.getResonanceAt());
        }
        return ApiResponse.success(data.isEmpty() ? null : data);
    }
}