package com.campus.campusqaservice.service;

import com.campus.campusqapojo.vo.AnswererRankVO;
import com.campus.campusqapojo.vo.HotQuestionVO;

import java.util.List;

/**
 * ClassName: RankService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/24 13:36
 * Version:1.0
 */
public interface RankService {
    /**
     * 全量重算今日热榜（定时任务调用）
     */
    void refreshHotRank();

    /**
     * 查询热榜 TopN
     *
     * @param limit 返回条数
     */
    List<HotQuestionVO> getHotRank(int limit);

    /**
     * 全量重算优秀回答者榜（定时任务调用）
     */
    void refreshAnswererRank();

    /**
     * 查询优秀回答者榜 TopN
     *
     * @param limit 返回条数
     */
    List<AnswererRankVO> getAnswererRank(int limit);
}
