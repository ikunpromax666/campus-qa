package com.campus.campusqaweb.config;

/**
 * ClassName: AutoFillMetaObjectHandler
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 16:36
 * Version:1.0
 */


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 只给标了 fill=INSERT 或 fill=INSERT_UPDATE 的字段赋值
        this.strictInsertFill(metaObject, "createTime",
                LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime",
                LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 只给标了 fill=INSERT_UPDATE 的字段赋值
        this.strictUpdateFill(metaObject, "updateTime",
                LocalDateTime.class, LocalDateTime.now());
    }
}