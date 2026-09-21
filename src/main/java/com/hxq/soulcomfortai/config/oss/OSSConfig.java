package com.hxq.soulcomfortai.config.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端装配。
 * 仅当 oss.enabled=true 且 AccessKey/Secret 均非空时才创建 OSS Bean；
 * 任一条件不满足时不注册 Bean，由 AvatarService 通过 ObjectProvider 感知可用性并降级，
 * 避免凭据缺失导致应用启动失败。
 */
@Configuration
public class OSSConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "oss", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${oss.access-key-id:}'.trim().length() > 0 && '${oss.access-key-secret:}'.trim().length() > 0")
    public OSS ossClient(OSSProperties props) {
        return new OSSClientBuilder().build(
                props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
    }
}