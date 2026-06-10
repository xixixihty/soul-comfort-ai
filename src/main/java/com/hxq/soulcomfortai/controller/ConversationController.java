package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.dto.request.CreateConversationRequest;
import com.hxq.soulcomfortai.dto.request.EditMessageRequest;
import com.hxq.soulcomfortai.dto.request.RenameRequest;
import com.hxq.soulcomfortai.dto.response.ConversationVO;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.service.ConversationService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationVO> create(@RequestAttribute("userId") String userId,
                                              @RequestBody CreateConversationRequest request) {
        ConversationVO vo = conversationService.create(userId, request.getTitle(), request.getTag());
        return ApiResponse.success(vo);
    }

    @GetMapping("/{id}")
    public ApiResponse<ConversationVO> getById(@RequestAttribute("userId") String userId,
                                               @PathVariable String id) {
        ConversationVO vo = conversationService.findById(id, userId);
        return ApiResponse.success(vo);
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<ConversationVO>> list(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<ConversationVO> result = conversationService.list(userId, page, size);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/title")
    public ApiResponse<ConversationVO> rename(@RequestAttribute("userId") String userId,
                                              @PathVariable String id,
                                              @RequestBody RenameRequest request) {
        ConversationVO vo = conversationService.rename(id, userId, request.getTitle());
        return ApiResponse.success(vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestAttribute("userId") String userId,
                                    @PathVariable String id) {
        conversationService.delete(id, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{convId}/suggested-questions")
    public ApiResponse<Map<String, Object>> getSuggestedQuestions(
            @RequestAttribute("userId") String userId,
            @PathVariable String convId) {
        return ApiResponse.success(conversationService.getSuggestedQuestions(convId, userId));
    }

    @PutMapping("/{convId}/messages/{messageId}/revoke")
    public ApiResponse<Map<String, Object>> revokeMessage(
            @RequestAttribute("userId") String userId,
            @PathVariable String convId,
            @PathVariable String messageId) {
        return ApiResponse.success(conversationService.revokeMessage(convId, userId, messageId));
    }

    @PutMapping("/{convId}/messages/{messageId}")
    public ApiResponse<Map<String, Object>> editMessage(
            @RequestAttribute("userId") String userId,
            @PathVariable String convId,
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ApiResponse.success(conversationService.editMessage(convId, userId, messageId, request.getContent()));
    }

    @PutMapping("/{id}/tag")
    public ApiResponse<ConversationVO> updateTag(@RequestAttribute("userId") String userId,
                                                  @PathVariable String id,
                                                  @RequestBody Map<String, String> body) {
        String tag = body.get("tag");
        return ApiResponse.success(conversationService.updateTag(id, userId, tag));
    }

    @GetMapping("/listByTag")
    public ApiResponse<PageResult<ConversationVO>> listByTag(
            @RequestAttribute("userId") String userId,
            @RequestParam String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(conversationService.listByTag(userId, tag, page, size));
    }

    @GetMapping("/tags")
    public ApiResponse<List<String>> getUserTags(@RequestAttribute("userId") String userId) {
        return ApiResponse.success(conversationService.getUserTags(userId));
    }
}