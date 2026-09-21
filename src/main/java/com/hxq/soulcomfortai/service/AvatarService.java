package com.hxq.soulcomfortai.service;

import com.aliyun.oss.OSS;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.config.oss.OSSProperties;
import com.hxq.soulcomfortai.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户头像上传服务：文件存入阿里云 OSS，图片地址写入 Redis（key：avatarKey(userId)）。
 * OSS 未启用或上传失败时优雅降级，不阻塞业务。
 */
@Slf4j
@Service
public class AvatarService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long AVATAR_TTL_SECONDS = 365L * 24 * 60 * 60;

    private final ObjectProvider<OSS> ossProvider;
    private final OSSProperties ossProperties;
    private final StringRedisTemplate redis;

    public AvatarService(ObjectProvider<OSS> ossProvider,
                         OSSProperties ossProperties,
                         StringRedisTemplate redis) {
        this.ossProvider = ossProvider;
        this.ossProperties = ossProperties;
        this.redis = redis;
    }

    /** 上传头像：写入 OSS 并把访问地址缓存到 Redis，返回可访问的图片地址 */
    public String uploadAvatar(String userId, MultipartFile file) {
        if (!ossProperties.isEnabled()) {
            throw new BusinessException(400, "头像存储服务未启用，请先在 application-local.yml 配置 oss.enabled=true");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(400, "头像大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "仅支持图片文件（jpg/png/webp/gif）");
        }

        String ext = extensionOf(contentType);
        String objectKey = "avatar/" + userId + "/" + UUID.randomUUID() + ext;

        try {
            OSS oss = ossProvider.getIfAvailable();
            if (oss == null) {
                throw new BusinessException(400, "头像存储服务未启用");
            }
            byte[] bytes = file.getBytes();
            oss.putObject(ossProperties.getBucket(), objectKey, new ByteArrayInputStream(bytes));

            String url = buildUrl(objectKey);
            redis.opsForValue().set(RedisConstants.avatarKey(userId), url, AVATAR_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("头像上传成功 userId={}, objectKey={}", userId, objectKey);
            return url;
        } catch (IOException e) {
            log.error("头像上传失败：读取文件失败 userId={}", userId, e);
            throw new BusinessException(500, "头像上传失败，请重试");
        } catch (RuntimeException e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            // 透传 OSS 具体错误原因（如 AK 被禁用/无权限/Key 无效），便于前端直接定位
            String reason = "OSS 连接异常";
            if (e instanceof com.aliyun.oss.OSSException) {
                String ossMsg = ((com.aliyun.oss.OSSException) e).getErrorMessage();
                if (ossMsg != null && !ossMsg.isBlank()) {
                    reason = ossMsg;
                }
            }
            log.error("头像上传到 OSS 失败 userId={}", userId, e);
            throw new BusinessException(500, "头像上传失败：" + reason);
        }
    }

    /** 读取用户头像地址（Redis 中存储的 OSS URL，未上传时为 null） */
    public String getAvatarUrl(String userId) {
        try {
            return redis.opsForValue().get(RedisConstants.avatarKey(userId));
        } catch (RuntimeException e) {
            log.warn("读取头像地址失败 userId={}", userId, e);
            return null;
        }
    }

    /** 删除用户头像记录（Redis 中的地址；OSS 文件保留由运维策略决定） */
    public void removeAvatar(String userId) {
        try {
            redis.delete(RedisConstants.avatarKey(userId));
        } catch (RuntimeException e) {
            log.warn("删除头像记录失败 userId={}", userId, e);
        }
    }

    private String buildUrl(String objectKey) {
        String prefix = ossProperties.getUrlPrefix();
        if (prefix != null && !prefix.isBlank()) {
            String trimmed = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            return trimmed + "/" + objectKey;
        }
        return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint() + "/" + objectKey;
    }

    private String extensionOf(String contentType) {
        String mime = contentType.toLowerCase(Locale.ROOT);
        if (mime.contains("png")) {
            return ".png";
        }
        if (mime.contains("webp")) {
            return ".webp";
        }
        if (mime.contains("gif")) {
            return ".gif";
        }
        return ".jpg";
    }
}