package com.campus.campusqamapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.campusqapojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * ClassName: UserMapper
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 15:01
 * Version:1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
