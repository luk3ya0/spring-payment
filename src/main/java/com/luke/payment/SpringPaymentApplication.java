package com.luke.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringPaymentApplication.class, args);
    }

}
