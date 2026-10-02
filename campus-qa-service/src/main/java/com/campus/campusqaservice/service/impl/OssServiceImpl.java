package com.campus.campusqaservice.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqaservice.config.OssProperties;
import com.campus.campusqaservice.service.OssService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

/**
 * ClassName: OssServiceImpl
 * Description: 阿里云 OSS 上传实现
 *
 * 面试高频问题：
 * Q: OSSClient 为什么做成全局单例，而不是每次上传 new 一个？
 * A: OSSClient 线程安全，内部维护连接池；每次 new 会反复建连/销毁，
 *    高并发下产生大量 TIME_WAIT、浪费资源——和数据库连接池复用是同一个道理。
 *    所以在 @PostConstruct 建一次，@PreDestroy 释放。
 *
 * Q: 文件名为什么用 UUID 重命名，不用原始文件名？
 * A: ① 两个用户都上传"头像.png"会互相覆盖；
 *    ② 原始文件名可能含中文/特殊字符/路径符号，拼 URL 有安全隐患且不可读。
 *    objectName = 目录/UUID.ext，目录按 userId 隔离，便于管理和清理。
 *
 * Author: SuperXia
 * Datetime: 2026/10/2
 * Version: 1.0
 */
@Service
public class OssServiceImpl implements OssService {

    private static final Logger log = LoggerFactory.getLogger(OssServiceImpl.class);

    private final OssProperties properties;
    private OSS ossClient;

    public OssServiceImpl(OssProperties properties) {
        this.properties = properties;
    }

    /**
     * 容器启动时创建一次客户端，全局复用。
     *
     * 面试考点（真实事故复盘）：@PostConstruct 里抛异常 = 整个应用启动失败
     * （OSSClientBuilder.build 虽不联网，但会校验凭证非空，AK 缺失直接 InvalidCredentialsException）。
     * OSS 是可选增强功能，凭证缺失应该"启动降级、用时报错"，而不是让登录/问答等核心功能陪葬。
     */
    @PostConstruct
    public void init() {
        if (isBlank(properties.getAccessKeyId()) || isBlank(properties.getAccessKeySecret())) {
            log.warn("阿里云 OSS 未配置（aliyun.oss.access-key-id/secret 为空），头像上传不可用，其余功能不受影响");
            return; // ossClient 保持 null，upload() 里拦截
        }
        String endpoint = properties.getEndpoint();
        if (endpoint != null && !endpoint.startsWith("http")) {
            endpoint = "https://" + endpoint;
        }
        ossClient = new OSSClientBuilder().build(
                endpoint, properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 容器关闭时释放连接池，防止资源泄漏 */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    @Override
    public String upload(String folder, String originalFilename, InputStream in) {
        // 降级拦截：init 时凭证为空则 client 未创建，走到这里说明功能被调用但环境未配置
        if (ossClient == null) {
            throw new BusinessException("文件上传服务未配置，请先配置阿里云 OSS（aliyun.oss.access-key-id/secret）");
        }
        // 提取扩展名（扩展名白名单由调用方 UserService 校验，这里只负责拼 objectName）
        String ext = "jpg";
        if (originalFilename != null && originalFilename.lastIndexOf('.') >= 0) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        // objectName = 目录/UUID.ext：UUID 防重名覆盖，也避免原始文件名的特殊字符进 URL
        String objectName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            ossClient.putObject(properties.getBucketName(), objectName, in);
        } catch (Exception e) {
            // OSS 异常（凭证错误/Bucket 不存在/网络不通）转业务异常给前端友好提示，
            // 不然会落进全局兜底 handler 返回 500"系统异常"，用户不知道该去检查配置
            log.error("OSS 上传失败, objectName={}", objectName, e);
            throw new BusinessException("文件上传失败，请检查 OSS 配置是否正确");
        }

        // 拼公共访问 URL：https://{bucket}.{endpoint主机}/{objectName}（Bucket 需公共读）
        String host = properties.getEndpoint().replaceFirst("^https?://", "");
        String url = "https://" + properties.getBucketName() + "." + host + "/" + objectName;
        log.info("OSS 上传成功: {}", url);
        return url;
    }
}
