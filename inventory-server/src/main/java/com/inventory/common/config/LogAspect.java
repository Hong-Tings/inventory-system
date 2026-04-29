package com.inventory.common.config;

import cn.dev33.satoken.stp.StpUtil;
import com.inventory.system.entity.SysOperationLog;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysOperationLogMapper;
import com.inventory.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    private static final Map<String, String> ACTION_MAP = Map.ofEntries(
            Map.entry("create", "新增"),
            Map.entry("save", "保存"),
            Map.entry("update", "更新"),
            Map.entry("delete", "作废"),
            Map.entry("voidOrder", "作废"),
            Map.entry("batchVoid", "批量作废"),
            Map.entry("batchDelete", "批量作废"),
            Map.entry("submit", "提交"),
            Map.entry("cancel", "取消"),
            Map.entry("approve", "审核"),
            Map.entry("adjust", "盘点调整"),
            Map.entry("addItem", "添加明细"),
            Map.entry("deleteItem", "删除明细"),
            Map.entry("updateItem", "更新明细"),
            Map.entry("updateDraft", "更新草稿"),
            Map.entry("restore", "恢复")
    );

    private static final Map<String, String> MODULE_MAP = Map.ofEntries(
            Map.entry("Product", "商品"),
            Map.entry("ProductCategory", "商品分类"),
            Map.entry("Supplier", "供应商"),
            Map.entry("Customer", "客户"),
            Map.entry("Warehouse", "仓库"),
            Map.entry("PurchaseOrder", "采购入库"),
            Map.entry("SalesOrder", "销售出库"),
            Map.entry("StockTake", "库存盘点"),
            Map.entry("Transfer", "库存调拨"),
            Map.entry("InventoryTransfer", "库存调拨"),
            Map.entry("Inventory", "库存"),
            Map.entry("InventoryLog", "库存流水"),
            Map.entry("User", "员工管理"),
            Map.entry("SysConfig", "系统配置"),
            Map.entry("Auth", "登录认证")
    );

    private final SysOperationLogMapper logMapper;
    private final SysUserMapper userMapper;

    public LogAspect(SysOperationLogMapper logMapper, SysUserMapper userMapper) {
        this.logMapper = logMapper;
        this.userMapper = userMapper;
    }

    @Pointcut("execution(* com.inventory.*.controller.*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();

        if (methodName.equals("page") || methodName.equals("getById")
                || methodName.equals("list") || methodName.equals("tree")
                || methodName.equals("stats")) {
            return joinPoint.proceed();
        }

        Object result = joinPoint.proceed();
        long cost = System.currentTimeMillis() - start;
        if (cost > 500 || methodName.matches("create|submit|cancel|save|update|delete|voidOrder|approve|adjust|restore")) {
            saveLog(joinPoint, methodName, result, cost);
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, String methodName, Object result, long cost) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest request = attrs.getRequest();

            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String className = sig.getDeclaringType().getSimpleName().replace("Controller", "");

            String actionDesc = ACTION_MAP.getOrDefault(methodName, methodName);
            String moduleDesc = MODULE_MAP.getOrDefault(className, className);

            // 提取目标信息
            String targetInfo = extractTarget(joinPoint.getArgs());
            String detail = actionDesc + moduleDesc;
            if (targetInfo != null) {
                detail += "：" + targetInfo;
            }

            // 操作人姓名
            String operatorName = "";
            try {
                long userId = StpUtil.getLoginIdAsLong();
                SysUser user = userMapper.selectById(userId);
                operatorName = user != null ? user.getRealName() : String.valueOf(userId);
            } catch (Exception ignored) {}

            SysOperationLog log = new SysOperationLog();
            log.setModule(moduleDesc);
            log.setAction(actionDesc);
            log.setDetail(detail);
            log.setTargetId(extractTargetId(request.getRequestURI()));
            log.setRequestUrl(request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setIp(request.getRemoteAddr());
            log.setCostTime((int) cost);
            log.setResult(1);
            log.setOperatorId(StpUtil.getLoginIdAsLong());
            log.setOperator(operatorName);
            log.setCreateTime(LocalDateTime.now());
            logMapper.insert(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响主业务
        }
    }

    private String extractTarget(Object[] args) {
        if (args == null || args.length == 0) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            // 跳过基础类型和包装类型
            if (arg instanceof Boolean || arg instanceof Number || arg instanceof String) continue;
            try {
                // 尝试反射获取名称类字段
                Class<?> clazz = arg.getClass();
                // 检查包名，只处理业务实体
                String pkg = clazz.getPackageName();
                if (!pkg.startsWith("com.inventory")) continue;

                // 尝试 orderNo、code、name、reason
                for (String field : new String[]{"orderNo", "code", "name", "reason"}) {
                    try {
                        Method getter = clazz.getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
                        Object val = getter.invoke(arg);
                        if (val != null && !val.toString().isEmpty()) {
                            return val.toString();
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String extractTargetId(String uri) {
        // 从 URL 提取最后一段数字 ID，如 /api/v1/product/123 → 123
        String[] parts = uri.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                Long.parseLong(parts[i]);
                return parts[i];
            } catch (NumberFormatException ignored) {}
        }
        return "";
    }
}
