package com.inventory.transfer;

import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.entity.InventoryTransferItem;
import com.inventory.transfer.mapper.InventoryTransferItemMapper;
import com.inventory.transfer.mapper.InventoryTransferMapper;
import com.inventory.transfer.service.TransferService;
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
class TransferServiceTest {

    @Mock private InventoryTransferMapper transferMapper;
    @Mock private InventoryTransferItemMapper transferItemMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryLogMapper inventoryLogMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ProductMapper productMapper;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(
            transferMapper, transferItemMapper, inventoryMapper,
            inventoryLogMapper, warehouseMapper, userMapper, productMapper
        );
    }

    private InventoryTransfer createTransfer(int status) {
        InventoryTransfer t = new InventoryTransfer();
        t.setId(1L);
        t.setFromWarehouseId(1L);
        t.setToWarehouseId(2L);
        t.setStatus(status);

        List<InventoryTransferItem> items = new ArrayList<>();
        InventoryTransferItem item = new InventoryTransferItem();
        item.setProductId(1L);
        item.setQuantity(20);
        items.add(item);
        t.setItems(items);
        return t;
    }

    private InventoryTransfer createDraft() {
        return createTransfer(OrderStatus.DRAFT);
    }

    @Test
    void submitThenApprove_shouldDeductFromSourceAndAddToDestination() {
        when(transferMapper.selectById(1L))
            .thenReturn(createDraft())
            .thenReturn(createTransfer(OrderStatus.PENDING));
        when(transferItemMapper.selectList(any())).thenReturn(createDraft().getItems());

        Inventory source = new Inventory();
        source.setId(1L); source.setProductId(1L); source.setWarehouseId(1L);
        source.setQuantity(50); source.setLockedQty(0);
        source.setCostPrice(new BigDecimal("10.00"));

        when(inventoryMapper.selectOne(any())).thenReturn(source, null);
        when(inventoryMapper.selectList(any())).thenReturn(List.of());

        service.submit(1L);
        verify(transferMapper).updateById(argThat(t -> t.getStatus() == OrderStatus.PENDING));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            service.approve(1L);
        }
        verify(inventoryMapper).updateById(argThat(i -> i.getQuantity() == 30));
        verify(inventoryMapper).insert(argThat(i ->
            i.getWarehouseId() == 2L && i.getQuantity() == 20));
    }

    @Test
    void approve_shouldThrowWhenStockInsufficient() {
        when(transferMapper.selectById(1L))
            .thenReturn(createDraft())
            .thenReturn(createTransfer(OrderStatus.PENDING));
        when(transferItemMapper.selectList(any())).thenReturn(createDraft().getItems());

        Inventory low = new Inventory();
        low.setId(1L); low.setProductId(1L); low.setWarehouseId(1L);
        low.setQuantity(10); low.setLockedQty(0);

        when(inventoryMapper.selectOne(any())).thenReturn(low);

        service.submit(1L);

        assertThrows(BusinessException.class, () -> {
            try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
                stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
                service.approve(1L);
            }
        });
    }

    @Test
    void testRejectReturnsToDraft() {
        when(transferMapper.selectById(1L))
            .thenReturn(createDraft())
            .thenReturn(createTransfer(OrderStatus.PENDING));

        service.submit(1L);
        verify(transferMapper).updateById(argThat(t -> t.getStatus() == OrderStatus.PENDING));

        service.reject(1L, "价格不合理");
        verify(transferMapper).updateById(argThat(t ->
            t.getStatus() == OrderStatus.DRAFT && t.getRemark().contains("驳回原因")
        ));
    }

    @Test
    void cancel_shouldReverseTransfer() {
        InventoryTransfer confirmed = createDraft();
        confirmed.setStatus(OrderStatus.CONFIRMED);
        when(transferMapper.selectById(1L)).thenReturn(confirmed);
        when(transferItemMapper.selectList(any())).thenReturn(confirmed.getItems());

        Inventory dest = new Inventory();
        dest.setId(2L); dest.setProductId(1L); dest.setWarehouseId(2L);
        dest.setQuantity(20);

        Inventory src = new Inventory();
        src.setId(1L); src.setProductId(1L); src.setWarehouseId(1L);
        src.setQuantity(30);

        when(inventoryMapper.selectOne(any())).thenReturn(dest, src);

        service.cancel(1L);

        verify(inventoryMapper).updateById(argThat(i -> i.getId() == 2L && i.getQuantity() == 0));
        verify(inventoryMapper).updateById(argThat(i -> i.getId() == 1L && i.getQuantity() == 50));
    }
}
