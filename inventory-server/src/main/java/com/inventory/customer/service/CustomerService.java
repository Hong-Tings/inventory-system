package com.inventory.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.exception.BusinessException;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.mapper.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import cn.hutool.core.date.DateUtil;
import java.util.Date;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final SalesOrderMapper salesOrderMapper;

    public CustomerService(CustomerMapper customerMapper, SalesOrderMapper salesOrderMapper) {
        this.customerMapper = customerMapper;
        this.salesOrderMapper = salesOrderMapper;
    }

    public Page<Customer> page(Page<Customer> page, String name, String contact, String phone, String address) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .like(name != null, Customer::getName, name)
                .like(contact != null, Customer::getContact, contact)
                .like(phone != null, Customer::getPhone, phone)
                .like(address != null, Customer::getAddress, address)
                .orderByDesc(Customer::getId);
        return customerMapper.selectPage(page, wrapper);
    }

    public List<Customer> listAll() {
        return customerMapper.selectList(
                new LambdaQueryWrapper<Customer>().eq(Customer::getStatus, 1)
                        .orderByAsc(Customer::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Customer customer) {
        customer.setCode(generateCustomerCode());
        customerMapper.insert(customer);
    }

    private synchronized String generateCustomerCode() {
        String prefix = "CT";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;

        String maxCode = customerMapper.selectMaxCodeByPrefix(likePrefix);
        int seq = 1;
        if (maxCode != null) {
            seq = Integer.parseInt(maxCode.substring(maxCode.length() - 3)) + 1;
        }

        String code = likePrefix + String.format("%03d", seq);
        while (customerMapper.countAllByCode(code) > 0) {
            seq++;
            code = likePrefix + String.format("%03d", seq);
        }
        return code;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Customer customer) {
        customerMapper.updateById(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long soCount = salesOrderMapper.selectCount(
                new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getCustomerId, id));
        if (soCount > 0) throw new BusinessException("该客户已被销售出库单引用，无法删除");

        customerMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new com.inventory.common.exception.BusinessException("客户不存在");
        c.setDeleted(0);
        customerMapper.updateById(c);
    }
}
