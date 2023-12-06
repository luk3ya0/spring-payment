package com.luke.payment.controller;

import com.luke.payment.entity.Product;
import com.luke.payment.service.ProductService;
import com.luke.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Api(tags = "Product Management")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @ApiOperation("Test Demo")
    @GetMapping("/test")
    public R test() {
        return R.ok()
                       .data("message", "hello")
                       .data("now", new Date());
    }

    @GetMapping("/list")
    public R list() {
        List<Product> list = productService.list();

        return R.ok().data("productList", list);
    }

}
