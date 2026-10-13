package com.exam.productservice.controller;

import com.exam.productservice.entity.Product;
import com.exam.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public Product updateProductById(@PathVariable Long id, @RequestBody Product request) {
        return productService.updateProductById(id, request);
    }

    @PostMapping("/{id}/sell")
    public Product sellProductById(@PathVariable Long id, @RequestBody Product request) {
        return productService.sellProduct(id, request);
    }
}
