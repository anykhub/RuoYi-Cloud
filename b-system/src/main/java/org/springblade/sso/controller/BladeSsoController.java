package org.springblade.sso.controller;

import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.TokenUtil;
import org.springblade.core.secure.AuthInfo;
import org.springblade.sso.entity.User;
import org.springblade.sso.service.IUserService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * B 系统 SSO 控制器
 * 处理 B跳A 的发出，与 A跳B 的接收
 */
@RestController
@RequestMapping("/blade-sso")
// @AllArgsConstructor // 注释掉，以便手动注入带有 @Value 的属性
public class BladeSsoController {

    private final IUserService userService;

    // 使用 @Value 注入属性，需要显式构造函数或不使用 @AllArgsConstructor
    @Value("${sso.internal.secret}")
    private String internalSecret;

    @Value("${a-system.url}")
    private String aSystemUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public BladeSsoController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * A 跳 B 时，接收 A 系统的 token，验证后自动建账并签发 B 系统 JWT
     *
     * @param request 包含 token 的请求体
     * @return 返回包含 AuthInfo (SpringBlade JWT) 的响应
     */
    @PostMapping("/login")
    public R<AuthInfo> login(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.trim().isEmpty()) {
            return R.fail("Token 不能为空");
        }

        // 调用 A 系统的内部验证接口
        String verifyUrl = aSystemUrl + "/sso/internal/verify-token";

        HttpHeaders headers = new HttpHeaders();
        // 强制校验 Header 中的 X-Internal-Secret
        headers.set("X-Internal-Secret", internalSecret);
        headers.set("Content-Type", "application/json");

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(verifyUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // 判断 A 系统返回的结果
            if (result != null && (Integer) result.get("code") == 200) {
                // 取出用户信息，其中应包含 email 等建账所需信息
                Map<String, Object> userInfo = (Map<String, Object>) result.get("data");

                // 自动建账或更新
                User localUser = userService.findOrCreateBySso(userInfo);

                // 构造 BladeUser
                BladeUser bladeUser = new BladeUser();
                bladeUser.setUserId(localUser.getId());
                bladeUser.setAccount(localUser.getAccount());
                bladeUser.setUserName(localUser.getName());
                // 其他必要的 BladeUser 属性可在此处补充

                // 使用 SpringBlade 的 TokenUtil 签发 JWT
                AuthInfo authInfo = TokenUtil.createAuthInfo(bladeUser);

                return R.data(authInfo);
            } else {
                return R.fail("A系统 Token 验证失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("SSO 登录异常：" + e.getMessage());
        }
    }
}
