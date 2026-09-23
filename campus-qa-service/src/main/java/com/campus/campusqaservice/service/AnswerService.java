package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.AnswerPublishDTO;
import com.campus.campusqapojo.dto.AnswerQueryDTO;
import com.campus.campusqapojo.dto.AnswerUpdateDTO;
import com.campus.campusqapojo.vo.AnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ClassName: AnswerService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/22 08:51
 * Version:1.0
 */
public interface AnswerService {

    void publishAnswer( AnswerPublishDTO dto);

    PageResultVO<AnswerVO> listAnswers( AnswerQueryDTO dto);

    void updateAnswer(Long id, @Valid AnswerUpdateDTO dto);

    void deleteAnswer(Long id);

    void acceptBestAnswer(Long answerId, Long questionId);
}
