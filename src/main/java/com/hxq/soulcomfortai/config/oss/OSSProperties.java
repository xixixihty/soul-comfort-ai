package com.hxq.soulcomfortai.config.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置（凭据走环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET，
 * 本机可覆写于 application-local.yml 的 oss.* 节）。
 * enabled=false 时后端不创建 OSSClient，头像上传接口返回"存储服务未启用"。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OSSProperties {

    /** 是否启用 OSS 头像存储（默认关闭，避免无凭据时启动失败） */
    private boolean enabled = false;
    /** 地域接入点，如 oss-cn-hangzhou.aliyuncs.com */
    private String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    /** Bucket 所在地域 id，如 cn-hangzhou（用于生成 region） */
    private String region = "cn-hangzhou";
    /** OSS Bucket 名称 */
    private String bucket = "";
    /** AccessKey ID（优先环境变量 OSS_ACCESS_KEY_ID） */
    private String accessKeyId = "";
    /** AccessKey Secret（优先环境变量 OSS_ACCESS_KEY_SECRET） */
    private String accessKeySecret = "";
    /**
     * 访问前缀：最终头像地址 = urlPrefix + "/" + objectKey。
     * 默认缺省时按 https://{bucket}.{endpoint} 拼接；若配置了自定义域名/CND 请显式填写。
     */
    private String urlPrefix = "";
}