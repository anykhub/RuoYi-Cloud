package org.springblade.sso.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.sso.entity.User;

import java.util.Map;

/**
 * 用户服务类
 */
public interface IUserService extends IService<User> {

    /**
     * 根据 SSO 返回的用户信息查找或创建用户
     * @param userInfo SSO 返回的用户信息
     * @return 返回用户实体
     */
    User findOrCreateBySso(Map<String, Object> userInfo);
}
