package com.campus.campusqaservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: OssProperties
 * Description: 阿里云 OSS 配置属性（对应 application.yml 的 aliyun.oss 前缀）
 *
 * 面试高频问题：
 * Q: 为什么用 @ConfigurationProperties 而不是一堆 @Value？
 * A: 一组相关配置聚合成一个对象，类型安全、便于整体传递（OssService 构造器只收一个对象）；
 *    新增配置项只需加字段。@Value 适合零散的单个配置。
 *
 * Q: AK/SK 怎么管理才安全？
 * A: 绝不能硬编码提交到 Git。开发环境放本地 application.yml（该文件不入库或用占位值），
 *    生产用环境变量 / Nacos 配置中心 / K8s Secret 注入。
 *
 * Author: SuperXia
 * Datetime: 2026/10/2
 * Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /** OSS 区域节点，如 https://oss-cn-hangzhou.aliyuncs.com */
    private String endpoint;

    /** AccessKey ID（阿里云控制台 RAM 创建，建议只授 OSS 权限） */
    private String accessKeyId;

    /** AccessKey Secret，敏感凭据，禁止提交到仓库 */
    private String accessKeySecret;

    /** Bucket 名称，需设置"公共读"权限，否则返回的 URL 无法直接访问 */
    private String bucketName;
}
