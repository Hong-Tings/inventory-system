package com.inventory.purchase;

import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.entity.PurchaseOrderItem;
import com.inventory.purchase.mapper.PurchaseOrderItemMapper;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.purchase.service.PurchaseOrderService;
import com.inventory.supplier.mapper.SupplierMapper;
import com.inventory.system.mapper.SysUserMapper;
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

import cn.dev33.satoken.stp.StpUtil;
import org.mockito.MockedStatic;

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

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        // 手动构造以解决 @InjectMocks + synchronized 的代理问题
        service = new PurchaseOrderService(
            purchaseOrderMapper, purchaseOrderItemMapper, inventoryMapper,
            inventoryLogMapper, supplierMapper, warehouseMapper, userMapper, productMapper
        );
    }

    private PurchaseOrder createOrder(int status) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setWarehouseId(1L);
        order.setOperatorId(1L);
        order.setStatus(status);

        List<PurchaseOrderItem> items = new ArrayList<>();
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setProductId(1L);
        item.setQuantity(100);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("1000.00"));
        items.add(item);
        order.setItems(items);
        return order;
    }

    @Test
    void submitThenApprove_shouldCreateInventory() {
        when(purchaseOrderMapper.selectById(1L))
            .thenReturn(createOrder(OrderStatus.DRAFT))
            .thenReturn(createOrder(OrderStatus.PENDING));
        when(purchaseOrderItemMapper.selectList(any())).thenReturn(createOrder(OrderStatus.DRAFT).getItems());
        // 无库存记录
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        // 首次查询批次不存在
        when(inventoryMapper.selectOne(any())).thenReturn(null);

        // 提交后为待审批
        service.submit(1L);
        verify(purchaseOrderMapper).updateById(argThat(o -> o.getStatus() == OrderStatus.PENDING));

        // 审核通过后创建库存记录
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            service.approve(1L);
        }
        verify(inventoryMapper).insert(argThat(inv ->
            inv.getQuantity() == 100 && inv.getProductId() == 1L && inv.getWarehouseId() == 1L
        ));
        verify(purchaseOrderMapper).updateById(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
    }

    @Test
    void submitThenApprove_withExistingInventory_shouldIncreaseQuantity() {
        when(purchaseOrderMapper.selectById(1L))
            .thenReturn(createOrder(OrderStatus.DRAFT))
            .thenReturn(createOrder(OrderStatus.PENDING));
        when(purchaseOrderItemMapper.selectList(any())).thenReturn(createOrder(OrderStatus.DRAFT).getItems());

        Inventory existing = new Inventory();
        existing.setId(1L);
        existing.setProductId(1L);
        existing.setWarehouseId(1L);
        existing.setQuantity(50);
        existing.setCostPrice(new BigDecimal("8.00"));

        when(inventoryMapper.selectList(any())).thenReturn(List.of(existing));
        when(inventoryMapper.selectOne(any())).thenReturn(existing);

        service.submit(1L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            service.approve(1L);
        }

        // 验证：updateById 至少被调用一次，且数量从50→150
        verify(inventoryMapper, atLeastOnce()).updateById(argThat(inv ->
            inv.getId() == 1L && inv.getQuantity() == 150
        ));
        // 验证：成本价被更新为 9.33
        verify(inventoryMapper, atLeastOnce()).updateById(argThat(inv ->
            inv.getCostPrice() != null &&
            inv.getCostPrice().setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(new BigDecimal("9.33")) == 0
        ));
    }

    @Test
    void submit_shouldThrowWhenOrderNotDraft() {
        when(purchaseOrderMapper.selectById(1L)).thenReturn(createOrder(OrderStatus.CONFIRMED));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(1L));
        assertEquals("当前状态不可提交", ex.getMessage());
    }

    @Test
    void testRejectReturnsToDraft() {
        when(purchaseOrderMapper.selectById(1L))
            .thenReturn(createOrder(OrderStatus.DRAFT))
            .thenReturn(createOrder(OrderStatus.PENDING));

        service.submit(1L);
        verify(purchaseOrderMapper).updateById(argThat(o -> o.getStatus() == OrderStatus.PENDING));

        service.reject(1L, "价格不合理");
        verify(purchaseOrderMapper).updateById(argThat(o ->
            o.getStatus() == OrderStatus.DRAFT && o.getRemark().contains("驳回原因")
        ));
    }

    @Test
    void cancel_shouldReverseInventory() {
        PurchaseOrder confirmed = createOrder(OrderStatus.CONFIRMED);
        when(purchaseOrderMapper.selectById(1L)).thenReturn(confirmed);
        when(purchaseOrderItemMapper.selectList(any())).thenReturn(confirmed.getItems());

        Inventory inv = new Inventory();
        inv.setId(1L); inv.setProductId(1L); inv.setWarehouseId(1L);
        inv.setQuantity(100); inv.setCostPrice(new BigDecimal("10.00"));

        when(inventoryMapper.selectOne(any())).thenReturn(inv);
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inv));

        service.cancel(1L);

        verify(inventoryMapper, atLeastOnce()).updateById(argThat(i -> i.getQuantity() == 0));
    }

    @Test
    void voidOrder_shouldRejectConfirmed() {
        when(purchaseOrderMapper.selectById(1L)).thenReturn(createOrder(OrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () -> service.voidOrder(1L, "test"));
    }

    @Test
    void voidOrder_shouldAllowForDraft() {
        when(purchaseOrderMapper.selectById(1L)).thenReturn(createOrder(OrderStatus.DRAFT));
        service.voidOrder(1L, "测试原因");
        verify(purchaseOrderMapper).updateById(argThat(o ->
            o.getStatus() == OrderStatus.VOIDED && o.getRemark().contains("测试原因")
        ));
    }
}
