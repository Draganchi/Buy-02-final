package com.gritlab.product_service.repositories;

import com.gritlab.product_service.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findById(String id);
    Optional<Product> findByName(String name);
    List<Product> findByUserId(String userId);
}
