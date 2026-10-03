package com.campus.campusqamapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.campusqapojo.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * ClassName: QuestionMapper
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 15:05
 * Version:1.0
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 浏览量原子增量回写（ViewCountSyncTask 用）
     * 为什么是 SQL 层 col = col + delta 而不是读出来再加：回写可能与其他并发写（如直接 +1 降级路径）同时发生，
     * read-modify-write 会互相覆盖；原子加不依赖读取时的旧值，天然无丢更新。
     */
    @Update("update question set view_count = view_count + #{delta} where id = #{id}")
    int incrementViewCount(@Param("id") Long id, @Param("delta") long delta);
}
