package com.lims.common.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据状态冲突"),

    LOGIN_FAIL(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已停用"),
    REPEAT_SUBMIT(1003, "请勿重复提交"),

    CUSTOMER_NOT_POOL(2001, "该客户不在公海,无法认领"),
    CUSTOMER_OWNED(2002, "客户已被认领"),
    CREDIT_LIMIT_EXCEED(2003, "超出客户授信额度"),

    STATUS_NOT_ALLOWED(3001, "当前状态不允许此操作"),
    APPROVAL_TASK_DONE(3002, "该审批任务已处理"),
    NOT_APPROVER(3003, "您不是当前节点的审批人"),

    PRICING_RULE_ERROR(4001, "计价公式执行失败"),
    FILE_UPLOAD_ERROR(5001, "文件上传失败");

    private final int code;
    private final String message;
}
