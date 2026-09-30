package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.TagCreateDTO;
import com.campus.campusqapojo.vo.AdminTagVO;

import java.util.List;

/**
 * ClassName: TagService
 * Description: 标签领域服务（管理端 CRUD + 前台查询共用，按领域不分调用方）
 *
 * 面试高频问题：
 *  Q: 为什么 TagService 不拆成 AdminTagService 和 FrontTagService？
 *  A: Service 按领域划分，不按调用方划分。标签的增删查是同一套业务规则
 *    （重名检查、引用检查、排序），管理端和前台只是入口不同、VO 装配不同。
 *    拆两份会出现两套重复的引用检查逻辑，维护时容易漏改。Controller 按调用方分
 *    （admin/ 下放 AdminTagController），Service 按领域分，各司其职。
 *
 *  Q: 标签为什么没有 update 方法？
 *  A: 设计文档 §3.8.3 标签只定义了 list/save/delete 三个接口，没有编辑。
 *    标签语义轻（只有 name），改名需求弱；且标签被 question_tag 引用，
 *    改名会让历史问题的标签显示突变。如未来要加，再加 update 方法即可，
 *    Service 接口可扩展不影响现有调用方。
 *
 * Author: SuperXia
 * Datetime :2026/9/30 18:00
 * Version:1.0
 */
public interface TagService {

    /** 标签列表（管理端全量，按创建时间倒序） */
    List<AdminTagVO> list();

    /** 新增标签（重名检查 + 插入） */
    void save(TagCreateDTO dto);

    /** 删除标签（引用检查 + 物理删除） */
    void delete(Long id);
}
