package com.campus.campusqaservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.campusqapojo.dto.QuestionPublishDTO;
import com.campus.campusqapojo.dto.QuestionQueryDTO;
import com.campus.campusqapojo.dto.QuestionUpdateDTO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.QuestionDetailVO;
import com.campus.campusqapojo.vo.QuestionListVO;
import jakarta.validation.Valid;

/**
 * ClassName: QuestionService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/20 17:11
 * Version:1.0
 */
public interface QuestionService {

    Long publishQuestion(QuestionPublishDTO dto);

    PageResultVO<QuestionListVO> list(@Valid QuestionQueryDTO dto);

    QuestionDetailVO detail(Long id);

    Void update(Long id, @Valid QuestionUpdateDTO dto);

    void delete(Long id);

    void close(Long id);
}
