package com.inventory.sales;

import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.entity.SalesOrderItem;
import com.inventory.sales.mapper.SalesOrderItemMapper;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.sales.service.SalesOrderService;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import org.mockito.MockedStatic;

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

    private SalesOrderService service;

    @BeforeEach
    void setUp() {
        service = new SalesOrderService(
            salesOrderMapper, salesOrderItemMapper, inventoryMapper,
            inventoryLogMapper, customerMapper, warehouseMapper, userMapper, productMapper
        );
    }

    private SalesOrder createOrder(int status) {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setWarehouseId(1L);
        order.setStatus(status);
        List<SalesOrderItem> items = new ArrayList<>();
        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(1L);
        item.setQuantity(30);
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setAmount(new BigDecimal("600.00"));
        items.add(item);
        order.setItems(items);
        return order;
    }

    private SalesOrder createDraftOrder() {
        return createOrder(OrderStatus.DRAFT);
    }

    @Test
    void submitThenApprove_shouldDeductFromBatch() {
        when(salesOrderMapper.selectById(1L))
            .thenReturn(createDraftOrder())
            .thenReturn(createOrder(OrderStatus.PENDING));
        when(salesOrderItemMapper.selectList(any())).thenReturn(createDraftOrder().getItems());

        Inventory batch = new Inventory();
        batch.setId(1L); batch.setProductId(1L); batch.setWarehouseId(1L);
        batch.setQuantity(50);

        when(inventoryMapper.selectList(any())).thenReturn(List.of(batch));

        service.submit(1L);
        verify(salesOrderMapper).updateById(argThat(o -> o.getStatus() == OrderStatus.PENDING));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            service.approve(1L);
        }
        verify(inventoryMapper).updateById(argThat(i -> i.getQuantity() == 20));
    }

    @Test
    void approve_shouldThrowWhenStockInsufficient() {
        when(salesOrderMapper.selectById(1L))
            .thenReturn(createDraftOrder())
            .thenReturn(createOrder(OrderStatus.PENDING));
        when(salesOrderItemMapper.selectList(any())).thenReturn(createDraftOrder().getItems());

        Inventory low = new Inventory();
        low.setId(1L); low.setProductId(1L); low.setWarehouseId(1L);
        low.setQuantity(10);

        when(inventoryMapper.selectList(any())).thenReturn(List.of(low));

        service.submit(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
                stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
                service.approve(1L);
            }
        });
        assertEquals("商品库存不足", ex.getMessage());
    }

    @Test
    void testRejectReturnsToDraft() {
        when(salesOrderMapper.selectById(1L))
            .thenReturn(createDraftOrder())
            .thenReturn(createOrder(OrderStatus.PENDING));

        service.submit(1L);
        verify(salesOrderMapper).updateById(argThat(o -> o.getStatus() == OrderStatus.PENDING));

        service.reject(1L, "价格不合理");
        verify(salesOrderMapper).updateById(argThat(o ->
            o.getStatus() == OrderStatus.DRAFT && o.getRemark().contains("驳回原因")
        ));
    }

    @Test
    void cancel_shouldRestoreInventory() {
        SalesOrder confirmed = createDraftOrder();
        confirmed.setStatus(OrderStatus.CONFIRMED);
        when(salesOrderMapper.selectById(1L)).thenReturn(confirmed);
        when(salesOrderItemMapper.selectList(any())).thenReturn(confirmed.getItems());

        Inventory inv = new Inventory();
        inv.setId(1L); inv.setProductId(1L); inv.setWarehouseId(1L);
        inv.setQuantity(70);

        when(inventoryMapper.selectOne(any())).thenReturn(inv);

        service.cancel(1L);

        verify(inventoryMapper).updateById(argThat(i -> i.getQuantity() == 100));
    }

    @Test
    void voidOrder_shouldRejectConfirmed() {
        SalesOrder confirmed = createDraftOrder();
        confirmed.setStatus(OrderStatus.CONFIRMED);
        when(salesOrderMapper.selectById(1L)).thenReturn(confirmed);

        assertThrows(BusinessException.class, () -> service.voidOrder(1L, "test"));
    }
}
