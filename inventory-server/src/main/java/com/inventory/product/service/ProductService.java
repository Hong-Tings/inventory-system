package com.inventory.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.entity.ProductCategory;
import com.inventory.product.mapper.ProductCategoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrderItem;
import com.inventory.purchase.mapper.PurchaseOrderItemMapper;
import com.inventory.sales.entity.SalesOrderItem;
import com.inventory.sales.mapper.SalesOrderItemMapper;
import com.inventory.stocktake.entity.StockTakeItem;
import com.inventory.stocktake.mapper.StockTakeItemMapper;
import com.inventory.transfer.entity.InventoryTransferItem;
import com.inventory.transfer.mapper.InventoryTransferItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import cn.hutool.core.date.DateUtil;
import java.util.Date;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductCategoryMapper categoryMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final InventoryTransferItemMapper transferItemMapper;
    private final StockTakeItemMapper stockTakeItemMapper;

    public ProductService(ProductMapper productMapper, InventoryMapper inventoryMapper,
                          ProductCategoryMapper categoryMapper,
                          PurchaseOrderItemMapper purchaseOrderItemMapper,
                          SalesOrderItemMapper salesOrderItemMapper,
                          InventoryTransferItemMapper transferItemMapper,
                          StockTakeItemMapper stockTakeItemMapper) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.categoryMapper = categoryMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.transferItemMapper = transferItemMapper;
        this.stockTakeItemMapper = stockTakeItemMapper;
    }

    public Page<Product> page(Page<Product> page, String name, String code, Integer status, Boolean alertOnly, Long warehouseId, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minSalePrice, BigDecimal maxSalePrice, String startDate, String endDate) {
        // 日期参数先判空再转换，避免 MyBatis-Plus 条件参数提前求值导致 NPE
        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(name != null, Product::getName, name)
                .like(code != null, Product::getCode, code)
                .eq(status != null, Product::getStatus, status)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .ge(minPrice != null, Product::getPurchasePrice, minPrice)
                .le(maxPrice != null, Product::getPurchasePrice, maxPrice)
                .ge(minSalePrice != null, Product::getSalePrice, minSalePrice)
                .le(maxSalePrice != null, Product::getSalePrice, maxSalePrice)
                .ge(startDateTime != null, Product::getCreateTime, startDateTime)
                .le(endDateTime != null, Product::getCreateTime, endDateTime)
                .orderByDesc(Product::getId);
        Page<Product> result = productMapper.selectPage(page, wrapper);
        for (Product p : result.getRecords()) {
            enrichInventory(p, warehouseId);
        }
        enrichCategoryNames(result.getRecords());
        // 仓库筛选
        if (warehouseId != null) {
            List<Long> ids = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>().eq(Inventory::getWarehouseId, warehouseId))
                    .stream().map(Inventory::getProductId).distinct().collect(Collectors.toList());
            List<Product> filtered = result.getRecords().stream()
                    .filter(p -> ids.contains(p.getId())).collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }
        if (Boolean.TRUE.equals(alertOnly)) {
            List<Product> filtered = result.getRecords().stream()
                    .filter(p -> "warning".equals(p.getAlertStatus()))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        } else if (Boolean.FALSE.equals(alertOnly)) {
            List<Product> filtered = result.getRecords().stream()
                    .filter(p -> "normal".equals(p.getAlertStatus()))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }
        return result;
    }

    public List<Product> list() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1).orderByDesc(Product::getId));
    }

    public List<Product> listAll() {
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>().orderByDesc(Product::getId));
        for (Product p : list) enrichInventory(p);
        return list;
    }

    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    private void enrichInventory(Product p) {
        enrichInventory(p, null);
    }

    private void enrichInventory(Product p, Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, p.getId());
        if (warehouseId != null) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }
        List<Inventory> invs = inventoryMapper.selectList(wrapper);
        int total = invs.stream().mapToInt(Inventory::getQuantity).sum();
        p.setInventoryQuantity(total);
        p.setAlertStatus(p.getMinStock() != null && total < p.getMinStock() ? "warning" : "normal");
    }

    private void enrichCategoryNames(List<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return;
        List<ProductCategory> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> nameMap = categories.stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
        for (Product p : products) {
            if (p.getCategoryId() != null) {
                p.setCategoryName(nameMap.get(p.getCategoryId()));
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Product product) {
        product.setCode(generateProductCode());
        productMapper.insert(product);
    }

    private synchronized String generateProductCode() {
        String prefix = "GD";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;

        String maxCode = productMapper.selectMaxCodeByPrefix(likePrefix);
        int seq = 1;
        if (maxCode != null) {
            seq = Integer.parseInt(maxCode.substring(maxCode.length() - 3)) + 1;
        }

        String code = likePrefix + String.format("%03d", seq);
        while (productMapper.countAllByCode(code) > 0) {
            seq++;
            code = likePrefix + String.format("%03d", seq);
        }
        return code;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Product product) { productMapper.updateById(product); }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查库存
        long invCount = inventoryMapper.selectCount(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getProductId, id));
        if (invCount > 0) throw new BusinessException("该商品存在库存记录，无法删除，请先清空库存");

        // 检查采购入库引用
        long poCount = purchaseOrderItemMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getProductId, id));
        if (poCount > 0) throw new BusinessException("该商品已被采购入库单引用，无法删除");

        // 检查销售出库引用
        long soCount = salesOrderItemMapper.selectCount(
                new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getProductId, id));
        if (soCount > 0) throw new BusinessException("该商品已被销售出库单引用，无法删除");

        // 检查调拨引用
        long trCount = transferItemMapper.selectCount(
                new LambdaQueryWrapper<InventoryTransferItem>().eq(InventoryTransferItem::getProductId, id));
        if (trCount > 0) throw new BusinessException("该商品已被调拨单引用，无法删除");

        // 检查盘点引用
        long stCount = stockTakeItemMapper.selectCount(
                new LambdaQueryWrapper<StockTakeItem>().eq(StockTakeItem::getProductId, id));
        if (stCount > 0) throw new BusinessException("该商品已被盘点单引用，无法删除");

        productMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new com.inventory.common.exception.BusinessException("商品不存在");
        p.setDeleted(0);
        productMapper.updateById(p);
    }

}
