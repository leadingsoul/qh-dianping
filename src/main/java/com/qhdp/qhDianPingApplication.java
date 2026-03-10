package com.qhdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.qhdp.mapper")
@SpringBootApplication
public class qhDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(qhDianPingApplication.class, args);
    }

}
