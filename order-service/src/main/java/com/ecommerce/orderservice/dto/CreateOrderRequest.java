package com.ecommerce.orderservice.dto;

public class CreateOrderRequest {
    private String productId;
    private Integer quantity;
    private String customerId;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String productId, Integer quantity, String customerId) {
        this.productId = productId;
        this.quantity = quantity;
        this.customerId = customerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
