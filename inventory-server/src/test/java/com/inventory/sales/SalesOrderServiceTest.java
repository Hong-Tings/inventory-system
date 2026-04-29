package com.inventory.sales;

import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.entity.SalesOrderItem;
import com.inventory.sales.mapper.SalesOrderItemMapper;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.sales.service.SalesOrderService;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private SalesOrderItemMapper salesOrderItemMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryLogMapper inventoryLogMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private SalesOrderService service;

    private SalesOrder order;
    private List<SalesOrderItem> items;

    @BeforeEach
    void setUp() {
        order = new SalesOrder();
        order.setId(1L);
        order.setWarehouseId(1L);
        order.setStatus(OrderStatus.DRAFT);

        items = new ArrayList<>();
        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(1L);
        item.setQuantity(30);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setAmount(new BigDecimal("600.00"));
        items.add(item);
        order.setItems(items);
    }

    @Test
    void submit_shouldDeductInventoryFIFO() {
        when(salesOrderMapper.selectById(1L)).thenReturn(order);
        when(salesOrderItemMapper.selectList(any())).thenReturn(items);

        // 模拟两个批次（FIFO 按 ID 正序）
        Inventory batch1 = new Inventory();
        batch1.setId(1L); batch1.setProductId(1L); batch1.setWarehouseId(1L);
        batch1.setQuantity(50); batch1.setBatchNo("B001");

        Inventory batch2 = new Inventory();
        batch2.setId(2L); batch2.setProductId(1L); batch2.setWarehouseId(1L);
        batch2.setQuantity(50); batch2.setBatchNo("B002");

        when(inventoryMapper.selectList(any())).thenReturn(List.of(batch1, batch2));

        service.submit(1L);

        // 验证：FIFO 扣减，先扣 batch1 的 30 个
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper, times(2)).updateById(captor.capture());

        List<Inventory> updated = captor.getAllValues();
        assertEquals(20, updated.get(0).getQuantity(), "batch1 应剩余 20"); // 50-30
        assertEquals(50, updated.get(1).getQuantity(), "batch2 应不变");
    }

    @Test
    void submit_shouldThrowWhenStockInsufficient() {
        when(salesOrderMapper.selectById(1L)).thenReturn(order);
        when(salesOrderItemMapper.selectList(any())).thenReturn(items);

        // 库存不足
        Inventory low = new Inventory();
        low.setId(1L); low.setProductId(1L); low.setWarehouseId(1L);
        low.setQuantity(10);

        when(inventoryMapper.selectList(any())).thenReturn(List.of(low));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(1L));
        assertEquals("商品库存不足", ex.getMessage());
    }

    @Test
    void cancel_shouldRestoreInventory() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(salesOrderMapper.selectById(1L)).thenReturn(order);
        when(salesOrderItemMapper.selectList(any())).thenReturn(items);

        Inventory inv = new Inventory();
        inv.setId(1L); inv.setProductId(1L); inv.setWarehouseId(1L);
        inv.setQuantity(70);

        when(inventoryMapper.selectOne(any())).thenReturn(inv);

        service.cancel(1L);

        verify(inventoryMapper).updateById(argThat(i -> i.getQuantity() == 100));
    }

    @Test
    void voidOrder_shouldRejectConfirmedOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(salesOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.voidOrder(1L, "测试"));
        assertEquals("已确认的单据不可作废", ex.getMessage());
    }
}
