package com.hxq.soulcomfortai.config;

import com.hxq.soulcomfortai.annotation.CurrentUser;
import com.hxq.soulcomfortai.exception.AuthException;
import com.hxq.soulcomfortai.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String USER_ID_ATTR = "userId";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException(2003, "未登录或登录已过期");
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new AuthException(2003, "Token无效");
            }
            String userId = jwtUtil.getUserIdFromToken(token);
            request.setAttribute(USER_ID_ATTR, userId);
        } catch (ExpiredJwtException e) {
            throw new AuthException(2002, "Token已过期，请重新登录");
        }

        return true;
    }
}