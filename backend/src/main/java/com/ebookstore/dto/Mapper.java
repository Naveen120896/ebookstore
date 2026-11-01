package com.ebookstore.dto;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entity -> DTO conversion. Always call inside a @Transactional service (lazy relations). */
public final class Mapper {
    private Mapper() {}

    public static UserDto user(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getGiftPoints());
    }

    public static CategoryDto category(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }

    public static BrandDto brand(Brand b) {
        return b == null ? null : new BrandDto(b.getId(), b.getName());
    }

    public static ProductDto product(Product p) {
        return new ProductDto(p.getId(), p.getTitle(), p.getAuthor(), p.getDescription(), p.getPrice(),
                p.getStock(), p.getImageUrl(), category(p.getCategory()), brand(p.getBrand()),
                LocalDate.now().plusDays(p.getDeliveryDays()));
    }

    public static AddressDto address(Address a) {
        return new AddressDto(a.getId(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                a.getPostalCode(), a.getCountry(), a.isDefaultAddress());
    }

    public static CartDto cart(Cart c) {
        List<CartItemDto> items = c.getItems().stream()
                .map(i -> new CartItemDto(product(i.getProduct()), i.getQuantity(),
                        i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal subtotal = items.stream().map(CartItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(items, subtotal);
    }

    public static OrderDto order(Order o) {
        List<OrderItemDto> items = o.getItems().stream()
                .map(i -> new OrderItemDto(i.getProduct().getId(), i.getProduct().getTitle(), i.getQuantity(),
                        i.getUnitPrice(), i.getExpectedDelivery()))
                .toList();
        return new OrderDto(o.getId(), o.getStatus().name(), items, o.getSubtotal(), o.getGiftPointsUsed(),
                o.getTotal(), o.getCreatedAt(), o.getCreatedAt().plusHours(48));
    }

    public static PaymentDto payment(Payment p, String message) {
        return new PaymentDto(p.getId(), p.getOrder().getId(), p.getMethod().name(), p.getAmount(),
                p.getStatus().name(), message);
    }
}
