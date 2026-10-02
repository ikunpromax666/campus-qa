package com.campus.campusqaservice.service;

import java.io.InputStream;

/**
 * ClassName: OssService
 * Description: 对象存储通用服务：只负责"把流放进 OSS、返回访问 URL"，不关心业务含义
 * （是头像还是问题配图，由调用方决定目录前缀和校验规则）
 *
 * Author: SuperXia
 * Datetime: 2026/10/2
 * Version: 1.0
 */
public interface OssService {

    /**
     * 上传文件到 OSS
     *
     * @param folder           存储目录前缀，如 avatar/1（按业务/用户分目录，便于管理）
     * @param originalFilename 原始文件名（用于提取扩展名）
     * @param in               文件输入流
     * @return 可公开访问的 URL
     */
    String upload(String folder, String originalFilename, InputStream in);
}
