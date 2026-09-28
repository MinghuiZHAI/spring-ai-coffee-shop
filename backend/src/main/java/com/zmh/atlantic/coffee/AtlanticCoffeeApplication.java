package com.zmh.atlantic.coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。@EnableScheduling：退款延迟到账（§2.3）、会话超时（总体设计 v1.3 修正 4）、
 * 订单状态模拟推进器（§2.4）三类定时任务的统一开关。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AtlanticCoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtlanticCoffeeApplication.class, args);
    }
}
