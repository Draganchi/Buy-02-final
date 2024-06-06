package com.gritlab.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gritlab.product_service.exceptions.ProductCollectionException;
import com.gritlab.product_service.models.OrderDTO;
import com.gritlab.product_service.models.Product;
import com.gritlab.product_service.producer.SellerGainProducer;
import com.gritlab.product_service.producer.StockConfirmationProducer;
import com.gritlab.product_service.repositories.ProductRepository;
import com.gritlab.product_service.config.ValidateProduct;

import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final SellerGainProducer sellerGainProducer;
    private final StockConfirmationProducer stockConfirmationProducer;
    private final ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public ProductService(ProductRepository productRepository, SellerGainProducer sellerGainProducer, ObjectMapper objectMapper, StockConfirmationProducer stockConfirmationProducer) {
        this.productRepository = productRepository;
        this.sellerGainProducer = sellerGainProducer;
        this.objectMapper = objectMapper;
        this.stockConfirmationProducer = stockConfirmationProducer;
    }

    public void createProduct(Product product) throws ConstraintViolationException, ProductCollectionException {
        ValidateProduct.validateProduct(product);
        productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "totalAmountSold"));
    }

    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    public void updateProduct(String id, Product product) throws ProductCollectionException {
        Optional<Product> productOptional = productRepository.findById(id);

        ValidateProduct.validateProduct(product);

        if (productOptional.isPresent()) {
            Product productUpdate = productOptional.get();
            productUpdate.setName(product.getName());
            productUpdate.setDescription(product.getDescription());
            productUpdate.setPrice(product.getPrice());
            productUpdate.setQuantity(product.getQuantity());
            productUpdate.setUserId(product.getUserId());
            productRepository.save(productUpdate);
        } else {
            throw new ProductCollectionException(ProductCollectionException.NotFoundException(id));
        }
    }

    public void deleteProduct(String id) throws ProductCollectionException {
        Optional<Product> productOptional = productRepository.findById(id);
        if (!productOptional.isPresent()) {
            throw new ProductCollectionException(ProductCollectionException.NotFoundException(id));
        } else {
            kafkaTemplate.send("product_deletion", id);
            productRepository.deleteById(id);
        }
    }

    public Iterable<Product> getProductsByUserId(String userId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "totalAmountSold");
        return productRepository.findByUserId(userId, sort);
    }

    public void deleteProductsByUserId(String userId) {
        Iterable<Product> products = getProductsByUserId(userId);
        products.forEach(product -> {
            kafkaTemplate.send("product_deletion", product.getProductId());
            productRepository.delete(product);
        });
    }

    public boolean checkProductExists(String productId) {
        return productRepository.existsById(productId);
    }

    public void processOrder(OrderDTO order) {
        logger.info("Processing order with ID: {}", order.getId());
        Map<String, Long> productCount = order.getProductIds().stream()
                .filter(productId -> productId != null)
                .collect(Collectors.groupingBy(productId -> productId, Collectors.counting()));
        logger.debug("Product count for order: {}", productCount);

        productCount.forEach((productId, count) -> {
            productRepository.findById(productId).ifPresent(product -> {
                product.setQuantity(product.getQuantity() - count.intValue());
                product.setTotalAmountSold(product.getTotalAmountSold() + count.intValue());
                productRepository.save(product);
                logger.info("Updated quantity for product ID {}: new quantity {}", productId, product.getQuantity());
            });
        });

        List<Product> products = productRepository.findAllById(order.getProductIds());
        logger.debug("Retrieved products details for order: {}", products);

        Map<String, BigDecimal> sellerGains = calculateGainsPerSeller(products, productCount);
        logger.info("Calculated seller gains for order: {}", sellerGains);

        updateProductDetails(products, productCount);
        logger.info("Updated product details for order");

        sellerGains.forEach((userId, gain) -> {
            sellerGainProducer.sendSellerGain(userId, gain);
            logger.info("Sent seller gain to Kafka for sellerId {}: {}", userId, gain);
        });
    }

    private Map<String, BigDecimal> calculateGainsPerSeller(List<Product> products, Map<String, Long> productCount) {
        logger.debug("Calculating gains per seller");
        Map<String, BigDecimal> sellerGains = new HashMap<>();
        for (Product product : products) {
            Long count = productCount.get(product.getId());
            if (count != null) {
                BigDecimal gain = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(count));
                String sellerId = product.getUserId();
                sellerGains.merge(sellerId, gain, BigDecimal::add);
                logger.debug("Seller ID: {} - Gain: {}", sellerId, gain);
            }
        }
        return sellerGains;
    }

    private void updateProductDetails(List<Product> products, Map<String, Long> productCount) {
        logger.debug("Updating product details");
    }

    public void checkInStock(String orderJson) {
        logger.info("Checking stock for order: {}", orderJson);
        try {
            OrderDTO order = objectMapper.readValue(orderJson, OrderDTO.class);
            boolean allProductsInStock = true;
            Map<String, Integer> productQuantities = new HashMap<>();

            for (String productId : order.getProductIds()) {
                productQuantities.put(productId, productQuantities.getOrDefault(productId, 0) + 1);
            }

            for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
                Optional<Product> productOpt = productRepository.findById(entry.getKey());
                if (!productOpt.isPresent() || productOpt.get().getQuantity() < entry.getValue()) {
                    allProductsInStock = false;
                    break;
                }
            }

            if (allProductsInStock) {
                logger.info("All products are in stock for order ID: {}", order.getId());
                stockConfirmationProducer.sendStockConfirmation(order.getId(), "CONFIRMED");
            } else {
                logger.info("Not all products are in stock for order ID: {}", order.getId());
                stockConfirmationProducer.sendStockConfirmation(order.getId(), "DENIED");
            }
        } catch (IOException e) {
            logger.error("Error deserializing order JSON", e);
        }
    }
}

