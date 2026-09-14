package com.hxq.soulcomfortai.service;

import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.dto.response.LoginResponse;
import com.hxq.soulcomfortai.entity.User;
import com.hxq.soulcomfortai.exception.BusinessException;
import com.hxq.soulcomfortai.exception.RateLimitException;
import com.hxq.soulcomfortai.repository.AuthRepository;
import com.hxq.soulcomfortai.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final int MAX_LOGIN_FAILS = 5;
    private static final int LOGIN_LOCK_MINUTES = 15;

    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    public AuthService(AuthRepository authRepository, JwtUtil jwtUtil, StringRedisTemplate redis) {
        this.authRepository = authRepository;
        this.jwtUtil = jwtUtil;
        this.redis = redis;
    }

    public LoginResponse register(String username, String password, String nickname) {
        if (authRepository.existsByUsername(username)) {
            throw new BusinessException(1001, "用户名已存在");
        }

        if (password.length() < 8) {
            throw new BusinessException(1002, "密码长度不能少于8位");
        }

        String userId = authRepository.nextUserId();
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        long now = System.currentTimeMillis();

        User user = User.builder()
                .id(userId)
                .username(username)
                .passwordHash(passwordHash)
                .nickname(nickname != null && !nickname.isBlank() ? nickname : username)
                .createdAt(now)
                .build();

        authRepository.save(user);

        String token = jwtUtil.generateToken(userId, username);
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.fromUser(user);
        return LoginResponse.builder()
                .token(token)
                .user(withAvatar(userId, userInfo))
                .build();
    }

    public LoginResponse login(String username, String password) {
        checkLoginLock(username);

        User user = authRepository.findByUsername(username)
                .orElseThrow(() -> {
                    recordLoginFail(username);
                    throw new BusinessException(2001, "用户名或密码错误");
                });

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            recordLoginFail(username);
            throw new BusinessException(2001, "用户名或密码错误");
        }

        clearLoginFails(username);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .user(withAvatar(user.getId(), LoginResponse.UserInfo.fromUser(user)))
                .build();
    }

    private void checkLoginLock(String username) {
        String lockKey = RedisConstants.loginLockKey(username);
        String locked = redis.opsForValue().get(lockKey);
        if (locked != null) {
            Long ttl = redis.getExpire(lockKey, TimeUnit.SECONDS);
            throw new RateLimitException(
                    "登录尝试过于频繁，请" + (ttl != null && ttl > 0 ? ttl / 60 + 1 : LOGIN_LOCK_MINUTES) + "分钟后再试");
        }
    }

    private void recordLoginFail(String username) {
        String failKey = RedisConstants.loginFailKey(username);
        Long count = redis.opsForValue().increment(failKey);
        redis.expire(failKey, LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
        if (count != null && count >= MAX_LOGIN_FAILS) {
            String lockKey = RedisConstants.loginLockKey(username);
            redis.opsForValue().set(lockKey, "1", LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            redis.delete(failKey);
        }
    }

    private void clearLoginFails(String username) {
        redis.delete(RedisConstants.loginFailKey(username));
        redis.delete(RedisConstants.loginLockKey(username));
    }

    public LoginResponse.UserInfo getCurrentUser(String userId) {
        User user = authRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(1001, "用户不存在"));
        return withAvatar(userId, LoginResponse.UserInfo.fromUser(user));
    }

    /** 从 Redis 读取用户头像地址并填充到用户信息（头像地址存于 Redis 而非用户表） */
    private LoginResponse.UserInfo withAvatar(String userId, LoginResponse.UserInfo userInfo) {
        userInfo.setAvatarUrl(redis.opsForValue().get(RedisConstants.avatarKey(userId)));
        return userInfo;
    }
}