package com.hxq.soulcomfortai.dto.response;

import com.hxq.soulcomfortai.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String username;
        private String nickname;
        /** 用户头像地址（阿里云 OSS，存于 Redis，未上传时为 null） */
        private String avatarUrl;

        public static UserInfo fromUser(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .build();
        }
    }
}