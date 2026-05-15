package com.inventory.customer.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.entity.CustomerExportVO;
import com.inventory.customer.service.CustomerService;
import com.inventory.common.util.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户")
    @GetMapping("/page")
    public R<PageResult<Customer>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {
        return R.ok(PageResult.of(customerService.page(new Page<>(page, size), name, contact, phone, address)));
    }

    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public R<Customer> getById(@PathVariable Long id) {
        return R.ok(customerService.getById(id));
    }

    @Operation(summary = "查询所有启用的客户")
    @GetMapping("/list")
    public R<List<Customer>> list() {
        return R.ok(customerService.listAll());
    }

    @SaCheckRole("role_1")
    @Operation(summary = "新增客户")
    @PostMapping
    public R<Void> create(@RequestBody Customer customer) {
        customerService.save(customer);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.update(customer);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return R.ok();
    }

    @Operation(summary = "导出客户")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Customer> list = customerService.listAll();
        List<CustomerExportVO> voList = list.stream().map(s -> {
            CustomerExportVO vo = new CustomerExportVO();
            vo.setCode(s.getCode()); vo.setName(s.getName()); vo.setContact(s.getContact());
            vo.setPhone(s.getPhone()); vo.setAddress(s.getAddress());
            vo.setStatus(s.getStatus() != null && s.getStatus() == 1 ? "启用" : "停用");
            if (s.getCreateTime() != null) vo.setCreateTime(s.getCreateTime());
            if (s.getUpdateTime() != null) vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "客户列表", CustomerExportVO.class);
    }

    @SaCheckRole("role_1")
    @Operation(summary = "恢复已删除客户")
    @PutMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        customerService.restore(id);
        return R.ok();
    }
}
