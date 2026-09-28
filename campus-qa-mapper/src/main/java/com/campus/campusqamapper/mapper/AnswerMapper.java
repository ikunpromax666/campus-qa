package com.campus.campusqamapper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.campusqapojo.dto.AnswererStatDTO;
import com.campus.campusqapojo.entity.Answer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * ClassName: AnswerMapper
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 15:07
 * Version:1.0
 */
@Mapper
public interface AnswerMapper extends BaseMapper<Answer> {

    /**
     * 聚合统计每位回答者的贡献数据，按贡献分降序取前 limit 名
     * <p>
     * 为什么不用 QueryWrapper：Wrapper 的 select()/groupBy() 只能返回 List&lt;Map&lt;String, Object&gt;&gt;，
     * 拿字段要写 map.get("...")，既没有类型安全也会被拼写错误坑。聚合统计属于自定义 SQL 的领域。
     * <p>
     * 贡献分 = 被采纳数×10 + 累计获赞×2 + 回答数×1
     * 权重含义：被别人采纳是最强认可，其次是获赞，最后是"愿意回答"这件事本身
     * <p>
     * 注意：这里刻意不做时间衰减 —— 登录热榜要压老内容（防霸榜），而"优秀回答者"是长期贡献的累积，
     * 资历就该沉淀下来，衰减反而违背业务语义
     */
    @Select("""
            SELECT user_id                                                  AS userId,
                   COUNT(*)                                                 AS answerCount,
                   SUM(CASE WHEN is_accepted = 1 THEN 1 ELSE 0 END)         AS acceptedCount,
                   SUM(like_count)                                          AS likeSum,
                   SUM(CASE WHEN is_accepted = 1 THEN 1 ELSE 0 END) * 10
                       + SUM(like_count) * 2
                       + COUNT(*)                                           AS score
            FROM answer
            WHERE status = 0
            GROUP BY user_id
            ORDER BY score DESC
            LIMIT #{limit}
            """)
    List<AnswererStatDTO> selectAnswererStats(@Param("limit") int limit);
}
