package org.springblade.sso.controller;

import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.sso.cache.SsoTokenCache;
import org.springblade.sso.entity.User;
import org.springblade.sso.service.IUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * B 系统内部通信 SSO 控制器
 * 专供 A 系统后端及 B 系统前端刷新的接口
 */
@RestController
@RequestMapping("/blade-sso/internal")
public class BladeSsoInternalController {

    private final SsoTokenCache ssoTokenCache;
    private final IUserService userService;

    @Value("${sso.internal.secret}")
    private String internalSecret;

    @Value("${a-system.url}")
    private String aSystemUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public BladeSsoInternalController(SsoTokenCache ssoTokenCache, IUserService userService) {
        this.ssoTokenCache = ssoTokenCache;
        this.userService = userService;
    }

    /**
     * 供 A 系统校验的接口
     * 验证内部密钥，从 SsoTokenCache 取出并删除 token，返回 email 等信息
     *
     * @param request 包含 token 的请求体
     * @param secret  请求头包含 X-Internal-Secret 密钥
     * @return 返回验证结果，包含 email
     */
    @PostMapping("/verify-token")
    public R<String> verifyToken(@RequestBody Map<String, String> request, @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        // 强制校验 Header 中的 X-Internal-Secret，防范安全漏洞
        if (secret == null || !secret.equals(internalSecret)) {
            return R.fail("非法的内部通信密钥");
        }

        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return R.fail("Token 不能为空");
        }

        // 从缓存中获取并立刻删除 token（防重放）
        String email = ssoTokenCache.getAndRemove(token);

        if (email != null) {
            return R.data(email);
        } else {
            return R.fail("Token 无效或已过期");
        }
    }

    /**
     * 供 B 系统前端调用的无感刷新接口
     * 校验当前 B 系统自身的登录态，若有效，则携带内部密钥请求 A 系统的 JWT，返回给前端
     *
     * @return 返回 A 系统的 JWT 及其相关信息
     */
    @GetMapping("/refresh-a-token")
    public R<Map<String, Object>> refreshAToken() {
        // 校验当前 B 系统的登录态
        BladeUser user = AuthUtil.getUser();
        if (user == null) {
            return R.fail("当前 B 系统未登录或登录已过期");
        }

        // 获取当前用户的详细信息，从中提取 email
        User detailUser = userService.getById(user.getUserId());
        if (detailUser == null || detailUser.getEmail() == null) {
            return R.fail("用户信息或邮箱不存在");
        }

        String email = detailUser.getEmail();

        // 携带内部密钥，向 A 系统发起 issue-jwt 请求获取新的 Token
        String refreshUrl = aSystemUrl + "/sso/internal/issue-jwt";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.set("Content-Type", "application/json");

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(refreshUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // 假设 A 系统使用 AjaxResult 格式返回，code == 200 为成功
            if (result != null && (Integer) result.get("code") == 200) {
                // 提取 A 系统的新 JWT 信息并返回给 B 系统的前端
                Map<String, Object> tokenData = (Map<String, Object>) result.get("data");
                return R.data(tokenData);
            } else {
                return R.fail("刷新 A 系统 Token 失败：" + (result != null ? result.get("msg") : "未知错误"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("调用 A 系统刷新 Token 接口异常：" + e.getMessage());
        }
    }
}
