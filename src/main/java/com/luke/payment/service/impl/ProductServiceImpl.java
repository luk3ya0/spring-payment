package com.luke.payment.service.impl;

import com.luke.payment.entity.Product;
import com.luke.payment.mapper.ProductMapper;
import com.luke.payment.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
