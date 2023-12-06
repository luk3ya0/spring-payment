package com.luke.payment.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Product Management")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @ApiOperation("Test Demo")
    @GetMapping("/test")
    public String test() {
        return "hello world";
    }

}
