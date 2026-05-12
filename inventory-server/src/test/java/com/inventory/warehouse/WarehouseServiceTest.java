package com.inventory.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.transfer.mapper.InventoryTransferMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import com.inventory.warehouse.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private WarehouseMapper warehouseMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private InventoryTransferMapper transferMapper;
    @Mock private ProductMapper productMapper;

    private WarehouseService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseService(warehouseMapper, inventoryMapper,
                purchaseOrderMapper, salesOrderMapper, transferMapper, productMapper);
    }

    @Test
    void enrichStats_shouldCalculateProductCountAndTotalAmount() {
        Warehouse w = new Warehouse();
        w.setId(1L);
        when(warehouseMapper.selectPage(any(), any())).thenAnswer(inv -> {
            var page = inv.getArgument(0, com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            page.setRecords(List.of(w));
            return page;
        });

        Inventory inv1 = new Inventory();
        inv1.setId(1L); inv1.setProductId(1L); inv1.setWarehouseId(1L);
        inv1.setQuantity(100); inv1.setCostPrice(new BigDecimal("10.00"));

        Inventory inv2 = new Inventory();
        inv2.setId(2L); inv2.setProductId(2L); inv2.setWarehouseId(1L);
        inv2.setQuantity(50); inv2.setCostPrice(new BigDecimal("20.00"));

        when(inventoryMapper.selectList(any())).thenReturn(List.of(inv1, inv2));

        // 调用 page 触发 enrichStats
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Warehouse>(1, 10);
        service.page(page, null, null, null, null, null, null, null);

        // 验证：商品总数 = 100 + 50 = 150
        assertEquals(150, w.getProductCount().intValue());
        // 验证：库存金额 = 100×10 + 50×20 = 2000
        assertEquals(0, new BigDecimal("2000").compareTo(w.getTotalAmount()));
    }

    @Test
    void delete_shouldThrowWhenInventoryExists() {
        when(inventoryMapper.selectCount(any())).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("库存记录"));
    }

    @Test
    void delete_shouldThrowWhenPurchaseOrdersExist() {
        when(inventoryMapper.selectCount(any())).thenReturn(0L);
        when(purchaseOrderMapper.selectCount(any())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("采购入库单"));
    }

    @Test
    void delete_shouldSucceedWhenNoReferences() {
        when(inventoryMapper.selectCount(any())).thenReturn(0L);
        when(purchaseOrderMapper.selectCount(any())).thenReturn(0L);
        when(salesOrderMapper.selectCount(any())).thenReturn(0L);
        when(transferMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(warehouseMapper).deleteById(1L);
    }
}
