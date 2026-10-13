package com.exam.productservice.service;

import com.exam.productservice.entity.Product;
import com.exam.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;


    @Override
    @Cacheable(value = "products",key = "'all'")
    public List<Product> getProducts() {
        System.out.println("getProducts All");
        return productRepository.findAll();
    }

    @Override
    @Cacheable(value = "products",key = "#id")
    public Product getProductById(Long id) {
        System.out.println("getProducts By Id");
        return productRepository.findById(id).orElseThrow(()->new RuntimeException("Không tìm thấy sản phẩm"));
    }

    @Override
    @CacheEvict(value = "products", key = "#request.id")
    public Product updateProductById(Long id, Product request) {
        Product product =  productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy sản phẩm"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setQuantity(request.getQuantity());

        return productRepository.save(product);
    }

    @Override
    public Product sellProduct(Long id, Product request) {

        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Số lượng bán phải lớn hơn 0"
            );
        }
        String lockKey = "lock:medicine:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException(
                        "Hệ thống đang xử lý giao dịch này, vui lòng thử lại"
                );
            }

            Product product = productRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Không tìm thấy thuốc")
                    );

            if (product.getQuantity() < request.getQuantity()) {
                throw new IllegalStateException(
                        "Thuốc đã hết hàng hoặc không đủ số lượng"
                );
            }
            product.setQuantity(product.getQuantity() - request.getQuantity());
            return productRepository.save(product);

        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Giao dịch bị gián đoạn khi chờ Lock", e
            );
        } finally {
            // Chỉ mở Lock nếu chính thread hiện tại đang giữ nó
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}
