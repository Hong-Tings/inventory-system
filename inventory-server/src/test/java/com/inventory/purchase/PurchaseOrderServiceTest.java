package com.inventory.purchase;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.entity.PurchaseOrderItem;
import com.inventory.purchase.mapper.PurchaseOrderItemMapper;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.purchase.service.PurchaseOrderService;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.mapper.SupplierMapper;
import com.inventory.system.entity.SysUser;
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
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryLogMapper inventoryLogMapper;
    @Mock private SupplierMapper supplierMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private PurchaseOrderService service;

    private PurchaseOrder order;
    private List<PurchaseOrderItem> items;

    @BeforeEach
    void setUp() {
        order = new PurchaseOrder();
        order.setId(1L);
        order.setWarehouseId(1L);
        order.setOperatorId(1L);
        order.setStatus(OrderStatus.DRAFT);

        items = new ArrayList<>();
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductId(1L);
        item.setQuantity(100);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("1000.00"));
        items.add(item);

        order.setItems(items);
    }

    @Test
    void submit_shouldCreateInventoryAndSetCostPrice() {
        // 模拟：库存不存在（首次入库）
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(inventoryMapper.selectOne(any())).thenReturn(null);
        when(purchaseOrderMapper.insert(any())).thenReturn(1);
        when(purchaseOrderItemMapper.insert(any())).thenReturn(1);

        service.submit(1L);

        // 验证：库存记录被创建
        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper, atLeastOnce()).insert(invCaptor.capture());
        Inventory inserted = invCaptor.getAllValues().stream()
                .filter(i -> i.getId() == null && i.getQuantity() == 100)
                .findFirst().orElse(null);
        assertNotNull(inserted, "应创建库存记录");
        assertEquals(100, inserted.getQuantity());

        // 验证：订单状态改为已入库
        ArgumentCaptor<PurchaseOrder> orderCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderMapper).updateById(orderCaptor.capture());
        assertEquals(OrderStatus.CONFIRMED, orderCaptor.getValue().getStatus());
    }

    @Test
    void submit_withExistingInventory_shouldIncreaseQuantityAndRecalculateCost() {
        // 模拟：已有库存记录
        Inventory existing = new Inventory();
        existing.setId(1L);
        existing.setProductId(1L);
        existing.setWarehouseId(1L);
        existing.setQuantity(50);
        existing.setCostPrice(new BigDecimal("8.00"));

        // selectOne 返回已有库存
        when(inventoryMapper.selectOne(any())).thenReturn(existing);
        // selectList 查询已有批次（用于计算均价）
        when(inventoryMapper.selectList(any())).thenReturn(List.of(existing));

        service.submit(1L);

        // 验证：库存数量增加 50+100=150
        ArgumentCaptor<Inventory> updateCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(updateCaptor.capture());
        Inventory updated = updateCaptor.getValue();
        assertEquals(150, updated.getQuantity());

        // 验证：均价 = (50*8 + 100*10) / 150 = 1400/150 = 9.3333
        assertNotNull(updated.getCostPrice());
        assertEquals(0, new BigDecimal("9.33").compareTo(updated.getCostPrice().setScale(2, BigDecimal.ROUND_HALF_UP)),
                "加权平均成本价应为 9.33");
    }

    @Test
    void submit_shouldThrowWhenOrderNotDraft() {
        order.setStatus(OrderStatus.CONFIRMED);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(1L));
        assertEquals("当前状态不可提交", ex.getMessage());
    }

    @Test
    void cancel_shouldReverseInventoryForConfirmedOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(purchaseOrderMapper.selectById(1L)).thenReturn(order);

        // 模拟：查询入库明细
        when(purchaseOrderItemMapper.selectList(any())).thenReturn(items);

        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(1L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(100);
        inventory.setCostPrice(new BigDecimal("10.00"));

        when(inventoryMapper.selectOne(any())).thenReturn(inventory);
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inventory));

        service.cancel(1L);

        // 验证：库存减少
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getQuantity());
    }

    @Test
    void voidOrder_shouldRejectConfirmedOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(purchaseOrderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.voidOrder(1L, "测试作废"));
        assertEquals("已确认的单据不可作废", ex.getMessage());
    }

    @Test
    void voidOrder_shouldAllowForDraft() {
        order.setStatus(OrderStatus.DRAFT);
        when(purchaseOrderMapper.selectById(1L)).thenReturn(order);

        service.voidOrder(1L, "测试原因");

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals(OrderStatus.VOIDED, captor.getValue().getStatus());
        assertTrue(captor.getValue().getRemark().contains("测试原因"));
    }
}
