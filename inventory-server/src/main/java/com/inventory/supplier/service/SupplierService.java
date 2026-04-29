package com.inventory.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.exception.BusinessException;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.mapper.SupplierMapper;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import cn.hutool.core.date.DateUtil;
import java.util.Date;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    public SupplierService(SupplierMapper supplierMapper, PurchaseOrderMapper purchaseOrderMapper) {
        this.supplierMapper = supplierMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
    }

    public Page<Supplier> page(Page<Supplier> page, String name, String contact, String phone, String address) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                .like(name != null, Supplier::getName, name)
                .like(contact != null, Supplier::getContact, contact)
                .like(phone != null, Supplier::getPhone, phone)
                .like(address != null, Supplier::getAddress, address)
                .orderByDesc(Supplier::getId);
        return supplierMapper.selectPage(page, wrapper);
    }

    public List<Supplier> listAll() {
        return supplierMapper.selectList(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, 1)
                        .orderByAsc(Supplier::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Supplier supplier) {
        supplier.setCode(generateSupplierCode());
        supplierMapper.insert(supplier);
    }

    private synchronized String generateSupplierCode() {
        String prefix = "SP";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;

        String maxCode = supplierMapper.selectMaxCodeByPrefix(likePrefix);
        int seq = 1;
        if (maxCode != null) {
            seq = Integer.parseInt(maxCode.substring(maxCode.length() - 3)) + 1;
        }

        String code = likePrefix + String.format("%03d", seq);
        while (supplierMapper.countAllByCode(code) > 0) {
            seq++;
            code = likePrefix + String.format("%03d", seq);
        }
        return code;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Supplier supplier) {
        supplierMapper.updateById(supplier);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查采购入库引用
        long poCount = purchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getSupplierId, id));
        if (poCount > 0) throw new BusinessException("该供应商已被采购入库单引用，无法删除");

        supplierMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        Supplier s = supplierMapper.selectById(id);
        if (s == null) throw new com.inventory.common.exception.BusinessException("供应商不存在");
        s.setDeleted(0);
        supplierMapper.updateById(s);
    }
}
