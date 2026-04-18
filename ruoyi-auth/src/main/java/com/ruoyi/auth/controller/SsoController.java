package com.ruoyi.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * SSO 单点登录控制器
 * 处理 A 跳 B 的发出，与 B 跳 A 的接收
 */
@RestController
@RequestMapping("/sso")
public class SsoController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RemoteUserService remoteUserService;

    private static final String SSO_TOKEN_PREFIX = "sso:token:";

    /**
     * A 跳 B 时，生成短效 token
     * @return 返回短效 token
     */
    @PostMapping("/generate-token")
    public AjaxResult generateToken(javax.servlet.http.HttpServletRequest request) {
        // 从当前上下文中获取登录用户，而不是信任客户端传入的数据
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser == null || loginUser.getSysUser() == null) {
            return AjaxResult.error("User not authenticated");
        }

        SysUser sysUser = loginUser.getSysUser();

        Map<String, Object> tokenPayload = new HashMap<>();
        tokenPayload.put("userId", sysUser.getUserId());
        tokenPayload.put("userName", sysUser.getUserName());
        // 假设此处用邮箱作为关联键
        tokenPayload.put("email", sysUser.getEmail() != null ? sysUser.getEmail() : sysUser.getUserName());

        String token = IdUtils.fastUUID();
        // 设置30秒过期时间
        redisService.setCacheObject(SSO_TOKEN_PREFIX + token, tokenPayload, 30L, TimeUnit.SECONDS);
        return AjaxResult.success("Token generated successfully", token);
    }
}
