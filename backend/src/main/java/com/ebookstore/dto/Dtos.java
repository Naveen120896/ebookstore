package com.ebookstore.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public final class Dtos {
    private Dtos() {}

    // ---- auth ----
    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8) String password,
                                  @NotBlank String fullName) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserDto(Long id, String email, String fullName, int giftPoints) {}
    public record AuthResponse(String token, UserDto user) {}

    // ---- catalogue ----
    public record CategoryDto(Long id, String name) {}
    public record BrandDto(Long id, String name) {}
    public record ProductDto(Long id, String title, String author, String description, BigDecimal price,
                             int stock, String imageUrl, CategoryDto category, BrandDto brand,
                             LocalDate tentativeDeliveryDate) {}
    public record ProductPageDto(List<ProductDto> content, int page, int size, long totalElements) {}

    // ---- cart ----
    public record CartItemRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record CartItemDto(ProductDto product, int quantity, BigDecimal lineTotal) {}
    public record CartDto(List<CartItemDto> items, BigDecimal subtotal) {}

    // ---- address ----
    public record AddressDto(Long id, @NotBlank String line1, String line2, @NotBlank String city, String state,
                             @NotBlank String postalCode, @NotBlank String country, Boolean isDefault) {}

    // ---- orders & payments ----
    public record CheckoutRequest(@NotNull Long addressId, @Min(0) Integer giftPointsToRedeem) {}
    public record OrderItemDto(Long productId, String title, int quantity, BigDecimal unitPrice,
                               LocalDate expectedDelivery) {}
    public record OrderDto(Long id, String status, List<OrderItemDto> items, BigDecimal subtotal,
                           int giftPointsUsed, BigDecimal total, LocalDateTime createdAt,
                           LocalDateTime cancellableUntil) {}
    public record PaymentRequest(@NotBlank String method, String cardNumber, String cardHolder,
                                 String expiry, String cvv) {}
    public record PaymentDto(Long id, Long orderId, String method, BigDecimal amount, String status,
                             String message) {}
}
