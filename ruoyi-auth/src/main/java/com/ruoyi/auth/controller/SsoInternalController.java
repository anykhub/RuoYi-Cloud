package com.ruoyi.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * SSO 内部通信控制器 (A 系统)
 * 供 B 系统调用的内部接口
 */
@RestController
@RequestMapping("/sso/internal")
public class SsoInternalController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RemoteUserService remoteUserService;

    private static final String SSO_TOKEN_PREFIX = "sso:token:";

    @Value("${sso.internal.secret}")
    private String internalSecret;

    /**
     * 校验内部密钥，从 Redis 取出短效 Token，取完立即删除
     *
     * @param request 请求包含短效 token
     * @param secret  请求头包含 X-Internal-Secret 密钥
     * @return 包含用户信息的 AjaxResult
     */
    @PostMapping("/verify-token")
    public AjaxResult verifyToken(@RequestBody Map<String, String> request, @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            return AjaxResult.error("Invalid internal secret");
        }

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.error("Token is required");
        }

        String cacheKey = SSO_TOKEN_PREFIX + token;
        Map<String, Object> userInfo = redisService.getCacheObject(cacheKey);

        if (userInfo != null) {
            // 验证通过，取完立即删除，防重放
            redisService.deleteObject(cacheKey);
            return AjaxResult.success("Token valid", userInfo);
        } else {
            return AjaxResult.error("Token is invalid or expired");
        }
    }

    /**
     * 供 B 系统后端无感刷新调用的接口
     * 校验内部密钥，根据传入的 email 查询用户并签发若依的 JWT
     *
     * @param request 请求体包含 email
     * @param secret  请求头包含 X-Internal-Secret 密钥
     * @return 返回若依的 JWT 信息
     */
    @PostMapping("/issue-jwt")
    public AjaxResult issueJwt(@RequestBody Map<String, String> request, @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(internalSecret)) {
            return AjaxResult.error("Invalid internal secret");
        }

        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return AjaxResult.error("Email is required");
        }

        // TODO: 正常情况下此处应根据 email 获取用户信息。为了演示，此处使用 email 作为用户名进行查询。
        // 实际项目中应修改为根据 email 查询用户的对应接口。
        R<LoginUser> userResult = remoteUserService.getUserInfo(email, SecurityConstants.INNER);

        if (userResult == null || userResult.getData() == null || R.FAIL == userResult.getCode()) {
            throw new ServiceException("SSO JWT 发行失败：找不到该用户");
        }

        LoginUser userInfo = userResult.getData();
        Map<String, Object> tokenMap = tokenService.createToken(userInfo);

        return AjaxResult.success("JWT 发行成功", tokenMap);
    }
}
