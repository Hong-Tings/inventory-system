package com.inventory.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryLogService {

    private final InventoryLogMapper inventoryLogMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper sysUserMapper;

    public InventoryLogService(InventoryLogMapper inventoryLogMapper, ProductMapper productMapper,
                               WarehouseMapper warehouseMapper, SysUserMapper sysUserMapper) {
        this.inventoryLogMapper = inventoryLogMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public Page<InventoryLog> page(Page<InventoryLog> page, Long productId, String productName, Long warehouseId,
                                   String changeType, String refOrderNo, String operatorName,
                                   LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<InventoryLog> wrapper = new LambdaQueryWrapper<InventoryLog>()
                .eq(productId != null, InventoryLog::getProductId, productId)
                .eq(warehouseId != null, InventoryLog::getWarehouseId, warehouseId)
                .eq(changeType != null && !changeType.isEmpty(), InventoryLog::getChangeType, changeType)
                .ge(startDate != null, InventoryLog::getCreateTime, startDate)
                .le(endDate != null, InventoryLog::getCreateTime, endDate)
                .orderByDesc(InventoryLog::getId);

        // 关键字搜索：商品名称/编码 或 关联单号（OR 关系）
        boolean hasProductKw = productName != null && !productName.isEmpty() && productId == null;
        boolean hasRefNo = refOrderNo != null && !refOrderNo.isEmpty();

        if (hasProductKw || hasRefNo) {
            List<Long> productIds = null;
            if (hasProductKw) {
                List<Product> matchedProducts = productMapper.selectList(
                        new LambdaQueryWrapper<Product>()
                                .like(Product::getName, productName)
                                .or().like(Product::getCode, productName));
                if (!matchedProducts.isEmpty()) {
                    productIds = matchedProducts.stream().map(Product::getId).collect(Collectors.toList());
                }
            }
            List<Long> finalProductIds = productIds;
            wrapper.and(w -> {
                boolean hasAny = false;
                if (hasRefNo) { w.like(InventoryLog::getRefOrderNo, refOrderNo); hasAny = true; }
                if (finalProductIds != null && !finalProductIds.isEmpty()) {
                    if (hasAny) w.or();
                    w.in(InventoryLog::getProductId, finalProductIds);
                    hasAny = true;
                }
                if (!hasAny) w.eq(InventoryLog::getId, -1); // 无匹配时返回空
            });
        }

        // 按操作人名称搜索：先查出匹配的用户ID，再过滤库存流水
        if (operatorName != null && !operatorName.isEmpty()) {
            List<SysUser> matchedUsers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, operatorName)
                            .or().like(SysUser::getUsername, operatorName)
            );
            if (!matchedUsers.isEmpty()) {
                List<Long> userIds = matchedUsers.stream().map(SysUser::getId).collect(Collectors.toList());
                wrapper.in(InventoryLog::getOperatorId, userIds);
            } else {
                return page;
            }
        }

        Page<InventoryLog> result = inventoryLogMapper.selectPage(page, wrapper);
        // 填充名称
        for (InventoryLog log : result.getRecords()) {
            if (log.getProductId() != null) {
                Product p = productMapper.selectById(log.getProductId());
                if (p != null) {
                    log.setProductName(p.getName());
                }
            }
            if (log.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectById(log.getWarehouseId());
                if (w != null) {
                    log.setWarehouseName(w.getName());
                }
            }
            if (log.getOperatorId() != null) {
                SysUser u = sysUserMapper.selectById(log.getOperatorId());
                if (u != null) {
                    log.setOperatorName(u.getRealName());
                }
            }
        }
        return result;
    }

    public List<InventoryLog> listAll() {
        List<InventoryLog> list = inventoryLogMapper.selectList(
                new LambdaQueryWrapper<InventoryLog>().orderByDesc(InventoryLog::getId));
        for (InventoryLog log : list) {
            if (log.getProductId() != null) {
                Product p = productMapper.selectById(log.getProductId());
                if (p != null) log.setProductName(p.getName());
            }
            if (log.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectById(log.getWarehouseId());
                if (w != null) log.setWarehouseName(w.getName());
            }
            if (log.getOperatorId() != null) {
                SysUser u = sysUserMapper.selectById(log.getOperatorId());
                if (u != null) log.setOperatorName(u.getRealName());
            }
        }
        return list;
    }
}
