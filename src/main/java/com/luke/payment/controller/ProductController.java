package com.luke.payment.controller;

import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Api(tags = "Product Management")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @ApiOperation("Test Demo")
    @GetMapping("/test")
    public R test() {
        return R.ok()
                       .data("message", "hello")
                       .data("now", new Date());
    }

}
