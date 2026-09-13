package com.lims.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lims.common.security.SecurityUtils;
import com.lims.module.system.entity.SysOperationLog;
import com.lims.module.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 操作日志切面: 自动记录所有 Controller 的写操作(POST/PUT/PATCH/DELETE)到 sys_operation_log。
 * GET 不记录; 通知已读、OnlyOffice 服务器回调、报价试算等不落库的动作忽略。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    /** [HTTP方法, 路径模式(* 匹配单段), 模块, 动作]; 先匹配先生效 */
    private static final String[][] RULES = {
            {"POST", "/auth/login", "认证", "登录"},
            {"POST", "/auth/logout", "认证", "登出"},

            {"POST", "/customers", "客户管理", "保存客户"},
            {"POST", "/customers/*/claim", "客户管理", "认领客户"},
            {"POST", "/customers/*/release", "客户管理", "退回公海"},
            {"POST", "/customers/*/transfer", "客户管理", "分配/转交"},
            {"POST", "/customers/*/status", "客户管理", "变更客户状态"},
            {"POST", "/customers/credit/adjust", "客户管理", "信用调整"},
            {"POST", "/customers/qualifications", "客户管理", "保存资质"},
            {"DELETE", "/customers/qualifications/*", "客户管理", "删除资质"},
            {"POST", "/customers/follows", "客户管理", "新增跟进"},

            {"POST", "/contracts", "合同管理", "保存合同"},
            {"POST", "/contracts/*/submit", "合同管理", "提交合同审批"},
            {"POST", "/contracts/*/status", "合同管理", "合同状态变更"},
            {"POST", "/contracts/payments", "合同管理", "登记收付款"},
            {"DELETE", "/contracts/payments/*", "合同管理", "删除收付款"},
            {"POST", "/contracts/changes", "合同管理", "申请合同变更"},

            {"POST", "/entrusts", "委托管理", "保存委托单"},
            {"POST", "/entrusts/from-quote/*", "委托管理", "报价单一键转委托"},
            {"POST", "/entrusts/*/submit-review", "委托管理", "提交合同评审"},
            {"POST", "/entrusts/*/progress", "委托管理", "状态推进"},
            {"POST", "/entrusts/*/cancel", "委托管理", "取消委托单"},
            {"POST", "/entrusts/*/urgent", "委托管理", "加急设置"},
            {"POST", "/entrusts/adjustments", "委托管理", "调账申请"},
            {"POST", "/entrusts/subcontracts", "委托管理", "分包申请"},

            {"POST", "/quotes", "报价管理", "保存报价单"},
            {"POST", "/quotes/*/submit", "报价管理", "提交报价审批"},
            {"POST", "/quotes/*/void", "报价管理", "作废报价单"},
            {"POST", "/quotes/rules", "报价管理", "保存计价规则"},

            {"POST", "/approval/act", "审批中心", "审批处理"},

            {"POST", "/files/upload", "文件", "上传文件"},

            {"POST", "/mobile/entrusts/*/points", "移动采样", "提交采样点位"},
            {"POST", "/mobile/entrusts/*/finish-sampling", "移动采样", "完成采样"}
    };

    /** 即使匹配不到规则也不记录的路径(纯个人/服务器间/不落库动作) */
    private static final Pattern[] IGNORE = {
            Pattern.compile("^/notifications(/.*)?$"),
            Pattern.compile("^/onlyoffice/callback(/.*)?$"),
            Pattern.compile("^/quotes/calculate$")
    };

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        try {
            recordLog(pjp, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 日志失败绝不影响业务
            log.warn("操作日志写入失败: {}", e.getMessage());
        }
        return result;
    }

    private void recordLog(ProceedingJoinPoint pjp, long cost) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        String httpMethod = request.getMethod();
        if ("GET".equalsIgnoreCase(httpMethod) || "HEAD".equalsIgnoreCase(httpMethod)
                || "OPTIONS".equalsIgnoreCase(httpMethod)) {
            return;
        }
        String path = stripContextPath(request);
        for (Pattern p : IGNORE) {
            if (p.matcher(path).matches()) {
                return;
            }
        }
        String[] rule = matchRule(httpMethod, path);
        String module;
        String action;
        if (rule != null) {
            module = rule[2];
            action = rule[3];
        } else {
            module = defaultModule(path);
            action = defaultAction(httpMethod, path);
        }

        SysOperationLog entity = new SysOperationLog();
        SecurityUtils.LoginUser user = SecurityUtils.currentOrNull();
        if (user != null) {
            entity.setUserId(user.getUserId());
            entity.setUsername(user.getUsername());
        }
        entity.setModule(module);
        entity.setAction(action);
        entity.setMethod(httpMethod + " " + path);
        entity.setParams(extractParams(pjp.getArgs()));
        entity.setIp(clientIp(request));
        entity.setCostMs((int) cost);
        operationLogMapper.insert(entity);
    }

    private String[] matchRule(String httpMethod, String path) {
        for (String[] rule : RULES) {
            if (rule[0].equalsIgnoreCase(httpMethod) && pathMatch(rule[1], path)) {
                return rule;
            }
        }
        return null;
    }

    /** 极简 ant 风格匹配: * 匹配单段, ** 匹配多段 */
    private boolean pathMatch(String pattern, String path) {
        String[] ps = pattern.split("/");
        String[] as = path.split("/");
        int pi = 0;
        int ai = 0;
        while (pi < ps.length && ai < as.length) {
            if ("**".equals(ps[pi])) {
                return true;
            }
            if ("*".equals(ps[pi]) || ps[pi].equals(as[ai])) {
                pi++;
                ai++;
            } else {
                return false;
            }
        }
        return pi == ps.length && ai == as.length;
    }

    private String defaultModule(String path) {
        String seg = path.length() > 1 && path.startsWith("/") ? path.substring(1) : path;
        int slash = seg.indexOf('/');
        if (slash >= 0) {
            seg = seg.substring(0, slash);
        }
        if (seg.isEmpty()) {
            return "系统";
        }
        return seg;
    }

    private String defaultAction(String httpMethod, String path) {
        String tail = path;
        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            tail = path.substring(slash + 1);
        }
        if ("DELETE".equalsIgnoreCase(httpMethod)) {
            return "删除 " + tail;
        }
        if ("PUT".equalsIgnoreCase(httpMethod) || "PATCH".equalsIgnoreCase(httpMethod)) {
            return "修改 " + tail;
        }
        return "操作 " + tail;
    }

    private String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && ctx.length() > 0 && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    private String extractParams(Object[] args) {
        List<Object> safe = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            safe.add(arg);
        }
        if (safe.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(safe);
            // 掩码敏感字段
            json = json.replaceAll("(?i)\"(password|oldPassword|newPassword)\"\\s*:\\s*\"[^\"]*\"",
                    "\"$1\":\"***\"");
            if (json.length() > 2000) {
                json = json.substring(0, 2000);
            }
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && xff.trim().length() > 0) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && real.trim().length() > 0) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
