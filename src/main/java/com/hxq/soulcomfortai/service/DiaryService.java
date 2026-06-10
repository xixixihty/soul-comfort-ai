package com.hxq.soulcomfortai.service;

import com.hxq.soulcomfortai.dto.response.DiaryVO;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.entity.Diary;
import com.hxq.soulcomfortai.exception.BusinessException;
import com.hxq.soulcomfortai.repository.DiaryRepository;
import org.springframework.stereotype.Service;

@Service
public class DiaryService {

    private final DiaryRepository diaryRepository;

    public DiaryService(DiaryRepository diaryRepository) {
        this.diaryRepository = diaryRepository;
    }

    public DiaryVO create(String userId, String title, String content, String mood) {
        long now = System.currentTimeMillis();
        String id = diaryRepository.nextId();

        Diary diary = Diary.builder()
                .id(id)
                .userId(userId)
                .title(title)
                .content(content)
                .mood(mood)
                .createdAt(now)
                .updatedAt(now)
                .build();

        diaryRepository.save(diary);
        return toVO(diary);
    }

    public DiaryVO update(String diaryId, String userId, String title, String content, String mood) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("日记不存在或已过期"));
        if (!diary.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该日记");
        }

        diary.setTitle(title);
        diary.setContent(content);
        diary.setMood(mood);
        diary.setUpdatedAt(System.currentTimeMillis());

        diaryRepository.update(diary);
        return toVO(diary);
    }

    public DiaryVO findById(String diaryId, String userId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("日记不存在或已过期"));
        if (!diary.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该日记");
        }
        return toVO(diary);
    }

    public PageResult<DiaryVO> list(String userId, int page, int size) {
        PageResult<Diary> result = diaryRepository.findByUserId(userId, page, size);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                result.getPage(),
                result.getSize()
        );
    }

    public void delete(String diaryId, String userId) {
        diaryRepository.delete(diaryId, userId);
    }

    private DiaryVO toVO(Diary diary) {
        return DiaryVO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .mood(diary.getMood())
                .aiResonance(diary.getAiResonance())
                .resonanceAt(diary.getResonanceAt())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}