package com.campus.campusqacommon.exception;

import com.campus.campusqacommon.result.ResultCode;
import lombok.Getter;

/**
 * ClassName: BusinessException
 * Description:业务异常：业务逻辑不满足时抛出
 * `BusinessException(ResultCode)` 推荐 ，错误码和 message 从枚举取
 * `BusinessException(String)` 只传文案，code 自动走 10000 `
 * BusinessException(Integer, String)` 极特殊情况自定义
 *
 * * 业务异常：业务逻辑不满足时抛出
 *  *
 *  * 面试高频问题：
 *  * Q: 自定义异常为什么继承 RuntimeException 而不是 Exception？
 *  * A: 因为 Spring 的 @Transactional 默认只对 RuntimeException 回滚。
 *  *    如果继承 Exception（checked exception），方法内 throw 后事务不会回滚，
 *  *    需要额外配置 rollbackFor=Exception.class，容易忘。
 *  *    项目里统一用 RuntimeException 体系。
 *  *
 *  * Q: 为什么不用 IllegalArgumentException？
 *  * A: IllegalArgumentException 语义是"参数不合法"，太窄；
 *  *    BusinessException 语义更广——任何业务逻辑不满足都能抛（库存不足、无权限、状态不对），
 *  *    而且扩展了 code 字段，可以做错误码区分。
 *
 * Author: SuperXia
 * Datetime :2026/9/11 16:17
 * Version:1.0
 */
@Getter
public class BusinessException extends RuntimeException {
    //业务错误码
    private final Integer code;

    /** 推荐用枚举，错误码集中管理
     * 这里一开始没看懂，后来查官方文档了解到：
     * public class BusinessException extends RuntimeException
     *                                     ↑
     *                               extends Exception
     *                                     ↑
     *                                 extends Throwable ← 所有异常的老祖宗
     *
     *     `Throwable` 类里有一个私有字段`detailMessage` ，还有一个构造函数和 getter：
     *
     *     public class Throwable {
     *          private String detailMessage;           // 存异常消息
     *
     *          public Throwable (String message) {     // ← 带 message 的构造函数
     *          this .detailMessage = message;
     *          }
     *          public String getMessage () {                // ← 取消息
     *              return detailMessage;
     *          }
     *      }
     *      `RuntimeException` 继承了这个构造函数，所以`RuntimeException(String message)` 是存在的。
     * */
    public BusinessException (ResultCode resultCode) {
        super(resultCode.getMessage());  // 调用 RuntimeException(String) 构造函数
        this.code = resultCode.getCode(); // 子类自己扩展的字段
    }

    /** 兼容：只传 message，code 走通用业务错误码 */
    public BusinessException (String message) {
        super (message);
        this .code = ResultCode.BUSINESS_ERROR.getCode();
    }
    /** 兜底：自定义 code + message */
    public BusinessException (Integer code, String message) {
        super (message);
        this .code = code;
    }
}
