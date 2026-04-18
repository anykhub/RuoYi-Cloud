package com.ruoyi.auth.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
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

    @Value("${sso.internal.secret}")
    private String internalSecret;

    @Value("${b-system.url}")
    private String bSystemUrl;

    private RestTemplate restTemplate = new RestTemplate();

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

    /**
     * B 跳 A 时，接收 B 系统发来的 token 并验证，签发若依 JWT
     * @param request 包含 B 系统传来的 token
     * @return 返回若依的 JWT
     */
    @PostMapping("/login-by-token")
    public AjaxResult loginByToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return AjaxResult.error("Token is required");
        }

        // 调用 B 系统的内部验证接口
        String verifyUrl = bSystemUrl + "/blade-sso/internal/verify-token";

        HttpHeaders headers = new HttpHeaders();
        // 强制校验 Header 中的 X-Internal-Secret，防范安全漏洞
        headers.set("X-Internal-Secret", internalSecret);
        headers.set("Content-Type", "application/json");

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(verifyUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            if (result != null && (Integer) result.get("code") == 200) {
                // 获取验证成功返回的用户 email
                String email = (String) result.get("data");

                // TODO: 正常情况下此处应根据 email 获取用户信息。为了演示，此处使用 email 作为用户名进行查询。
                // 实际项目中应修改为根据 email 查询用户的对应接口。
                R<LoginUser> userResult = remoteUserService.getUserInfo(email, SecurityConstants.INNER);

                if (userResult == null || userResult.getData() == null || R.FAIL == userResult.getCode()) {
                    throw new ServiceException("SSO 登录失败：在 A 系统中找不到该用户");
                }

                LoginUser userInfo = userResult.getData();

                // 签发若依 JWT 返回给前端
                Map<String, Object> tokenMap = tokenService.createToken(userInfo);
                return AjaxResult.success("登录成功", tokenMap);
            } else {
                return AjaxResult.error("SSO Token 验证失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("SSO 登录异常：" + e.getMessage());
        }
    }
}
