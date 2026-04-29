package com.inventory.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("进销存管理系统 API")
                        .version("1.0.0")
                        .description("采购入库、销售出库、库存盘点管理接口")
                        .license(new License().name("Apache 2.0")));
    }
}
