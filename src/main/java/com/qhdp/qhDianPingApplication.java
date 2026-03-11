package com.qhdp;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.qhdp.mapper")
@SpringBootApplication
@Slf4j
public class qhDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(qhDianPingApplication.class, args);
        log.info("项目启动成功!......");
    }

}
