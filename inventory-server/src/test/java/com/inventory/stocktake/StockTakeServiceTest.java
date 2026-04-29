package com.inventory.stocktake;

import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.stocktake.entity.StockTake;
import com.inventory.stocktake.entity.StockTakeItem;
import com.inventory.stocktake.mapper.StockTakeItemMapper;
import com.inventory.stocktake.mapper.StockTakeMapper;
import com.inventory.stocktake.service.StockTakeService;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTakeServiceTest {

    @Mock private StockTakeMapper stockTakeMapper;
    @Mock private StockTakeItemMapper stockTakeItemMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryLogMapper inventoryLogMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private StockTakeService service;

    @Test
    void adjust_shouldOnlyAffectSpecifiedWarehouse() {
        StockTake stockTake = new StockTake();
        stockTake.setId(1L);
        stockTake.setWarehouseId(1L);
        stockTake.setStatus(OrderStatus.STOCKTAKE_APPROVED);
        when(stockTakeMapper.selectById(1L)).thenReturn(stockTake);

        StockTakeItem item = new StockTakeItem();
        item.setProductId(1L);
        item.setBookQty(90);
        item.setActualQty(80);
        item.setDiffQty(-10);
        when(stockTakeItemMapper.selectList(any())).thenReturn(List.of(item));

        // 库存记录：仓库1有88个，仓库2有11个
        Inventory wh1 = new Inventory();
        wh1.setId(1L); wh1.setProductId(1L); wh1.setWarehouseId(1L);
        wh1.setQuantity(88); wh1.setCostPrice(new BigDecimal("10.00"));

        Inventory wh2 = new Inventory();
        wh2.setId(2L); wh2.setProductId(1L); wh2.setWarehouseId(2L);
        wh2.setQuantity(11); wh2.setCostPrice(new BigDecimal("10.00"));

        // adjust() 会先查一次库存（已有批次用于计算），再查一次（更新均价）
        when(inventoryMapper.selectList(any())).thenReturn(
                List.of(wh1),  // 第一次：查仓库1的记录
                List.of(wh1)   // 第二次：更新均价时再查
        );

        service.adjust(1L);

        // 验证：仓库1的库存调整为80
        verify(inventoryMapper).updateById(argThat(inv ->
                inv.getId() == 1L && inv.getQuantity() == 80));

        // 验证：仓库2没有被修改
        verify(inventoryMapper, never()).updateById(argThat(inv ->
                inv.getId() == 2L));

        // 验证状态流转为已调整
        verify(stockTakeMapper).updateById(argThat(st ->
                st.getStatus() == OrderStatus.STOCKTAKE_ADJUSTED));
    }

    @Test
    void adjust_shouldThrowWhenNotApproved() {
        StockTake stockTake = new StockTake();
        stockTake.setId(1L);
        stockTake.setStatus(OrderStatus.STOCKTAKE_IN_PROGRESS);
        when(stockTakeMapper.selectById(1L)).thenReturn(stockTake);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.adjust(1L));
        assertEquals("当前状态不可调整，请先审核", ex.getMessage());
    }

    @Test
    void voidOrder_shouldRejectAdjusted() {
        StockTake stockTake = new StockTake();
        stockTake.setId(1L);
        stockTake.setStatus(OrderStatus.STOCKTAKE_ADJUSTED);
        when(stockTakeMapper.selectById(1L)).thenReturn(stockTake);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.voidOrder(1L, "测试"));
        assertEquals("已调整的盘点单不可作废", ex.getMessage());
    }
}
