package org.springblade.sso.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.sso.entity.User;
import org.springblade.sso.mapper.UserMapper;
import org.springblade.sso.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 根据 SSO 传过来的用户信息进行建账或更新
     *
     * @param userInfo A系统传过来的用户数据，其中必须包含 email
     * @return 返回更新后或新建的 User
     */
    @Override
    public User findOrCreateBySso(Map<String, Object> userInfo) {
        // 从 A 系统返回的数据中提取 email（这里为演示直接作为 String 处理，根据实际情况调整）
        String email = (String) userInfo.get("email");
        if (email == null) {
            throw new IllegalArgumentException("SSO returned user info missing email");
        }

        // 根据 email 查询库中是否已有该用户
        User existingUser = baseMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));

        if (existingUser != null) {
            // 已存在，可以根据需要更新部分字段
            existingUser.setSource("sso");
            // 假设 A 系统传递了 sourceUserId
            if(userInfo.get("userId") != null){
                existingUser.setSourceUserId(String.valueOf(userInfo.get("userId")));
            }
            updateById(existingUser);
            return existingUser;
        } else {
            // 不存在则新建用户
            User newUser = new User();
            newUser.setEmail(email);
            // 将邮箱前缀作为默认的账户名
            newUser.setAccount(email.split("@")[0]);
            newUser.setName((String) userInfo.getOrDefault("userName", email.split("@")[0]));
            newUser.setSource("sso");
            if(userInfo.get("userId") != null){
                newUser.setSourceUserId(String.valueOf(userInfo.get("userId")));
            }
            save(newUser);
            return newUser;
        }
    }
}
