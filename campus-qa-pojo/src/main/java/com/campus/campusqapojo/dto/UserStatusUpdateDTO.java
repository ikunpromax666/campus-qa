package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName: UserStatusUpdateDTO
 * Description: 管理员启用/禁用用户的请求体
 * Author: SuperXia
 * Datetime :2026/9/29 16:10
 * Version:1.0
 */
@Data
public class UserStatusUpdateDTO {

    /**
     * 目标状态：0 正常、1 禁用
     *
     * ① 这里刻意【不接收用户 id】：id 由路径参数 /admin/user/{id}/status 提供。
     *    两处都放 id 会出现"两个真相源"——body 传 5、路径写 6 时以谁为准？
     *    这种歧义正是越权漏洞的温床。
     * ② 【不能用 @NotBlank】：它的约束类型是 CharSequence（字符串），
     *    标在 Integer/Long 上，运行期校验时会抛 UnexpectedTypeException → 返回 500 而不是 400。
     *    非字符串类型统一用 @NotNull。
     * ③ @Min/@Max 对 null 是"放行"的，所以 @NotNull 必须单独写，两者配合才完整。
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0（正常）或 1（禁用）")
    @Max(value = 1, message = "状态只能是 0（正常）或 1（禁用）")
    private Integer status;
}
