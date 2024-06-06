package com.gritlab.product_service.models;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@NoArgsConstructor
@Document(collection = "products")
@Data
public class Product{
    @Id
    private String id;

    @NotBlank(message="Product name can't be empty")
    private String name;

    @NotBlank(message="Product description can't be empty")
    private String description;

    @NotNull(message = "Product price cannot be null")
    @DecimalMin(value = "0.0", message = "Product price must be greater than or equal to 0")
    private Double price;

    @NotNull(message = "Product quantity cannot be null")
    @Min(value = 0, message = "Product quantity must be greater than or equal to 0")
    private int quantity;

    @NotBlank(message = "Product userId cannot be empty")
    private String userId;

    public Product(String id, String name, String description, Double price, int quantity, String userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.userId = userId;
    }
}
