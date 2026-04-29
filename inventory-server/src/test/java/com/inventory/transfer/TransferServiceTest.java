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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private InventoryTransferMapper transferMapper;
    @Mock private InventoryTransferItemMapper transferItemMapper;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryLogMapper inventoryLogMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private TransferService service;

    private InventoryTransfer transfer;
    private List<InventoryTransferItem> items;

    @BeforeEach
    void setUp() {
        transfer = new InventoryTransfer();
        transfer.setId(1L);
        transfer.setFromWarehouseId(1L);
        transfer.setToWarehouseId(2L);
        transfer.setStatus(OrderStatus.DRAFT);

        items = new ArrayList<>();
        InventoryTransferItem item = new InventoryTransferItem();
        item.setProductId(1L);
        item.setQuantity(20);
        items.add(item);
        transfer.setItems(items);
    }

    @Test
    void submit_shouldDeductFromSourceAndAddToDestination() {
        when(transferMapper.selectById(1L)).thenReturn(transfer);
        when(transferItemMapper.selectList(any())).thenReturn(items);

        // 源仓有库存
        Inventory source = new Inventory();
        source.setId(1L); source.setProductId(1L); source.setWarehouseId(1L);
        source.setQuantity(50); source.setLockedQty(0);
        source.setCostPrice(new BigDecimal("10.00"));

        // 目标仓无此商品
        when(inventoryMapper.selectOne(any())).thenReturn(source, null);

        // selectList 查询目标仓已有批次（均价计算）
        when(inventoryMapper.selectList(any())).thenReturn(List.of());

        service.submit(1L);

        // 验证：源仓扣减 50→30
        verify(inventoryMapper).updateById(argThat(i ->
                i.getId() == 1L && i.getQuantity() == 30));

        // 验证：目标仓新增 20
        verify(inventoryMapper).insert(argThat(i ->
                i.getWarehouseId() == 2L && i.getQuantity() == 20));
    }

    @Test
    void submit_shouldThrowWhenStockInsufficient() {
        when(transferMapper.selectById(1L)).thenReturn(transfer);
        when(transferItemMapper.selectList(any())).thenReturn(items);

        Inventory low = new Inventory();
        low.setId(1L); low.setProductId(1L); low.setWarehouseId(1L);
        low.setQuantity(10); low.setLockedQty(0);

        when(inventoryMapper.selectOne(any())).thenReturn(low);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(1L));
        assertEquals("商品库存不足", ex.getMessage());
    }

    @Test
    void cancel_shouldReverseTransfer() {
        transfer.setStatus(OrderStatus.CONFIRMED);
        when(transferMapper.selectById(1L)).thenReturn(transfer);
        when(transferItemMapper.selectList(any())).thenReturn(items);

        // 模拟源仓和目标仓的库存
        Inventory dest = new Inventory();
        dest.setId(2L); dest.setProductId(1L); dest.setWarehouseId(2L);
        dest.setQuantity(20);

        Inventory src = new Inventory();
        src.setId(1L); src.setProductId(1L); src.setWarehouseId(1L);
        src.setQuantity(30);

        when(inventoryMapper.selectOne(any())).thenReturn(dest, src);

        service.cancel(1L);

        // 验证：目标仓回滚 20→0
        verify(inventoryMapper).updateById(argThat(i ->
                i.getId() == 2L && i.getQuantity() == 0));

        // 验证：源仓恢复 30→50
        verify(inventoryMapper).updateById(argThat(i ->
                i.getId() == 1L && i.getQuantity() == 50));
    }
}
