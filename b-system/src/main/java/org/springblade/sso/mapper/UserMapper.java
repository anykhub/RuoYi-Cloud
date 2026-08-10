package org.springblade.sso.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.sso.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
