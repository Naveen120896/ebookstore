package com.ebookstore.controller;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> history(@AuthenticationPrincipal Long userId) {
        return orderService.history(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@AuthenticationPrincipal Long userId, @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(userId, request);
    }

    @GetMapping("/{id}")
    public OrderDto get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orderService.get(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orderService.cancel(userId, id);
    }

    @PostMapping("/{id}/payment")
    public PaymentDto pay(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                          @Valid @RequestBody PaymentRequest request) {
        return orderService.pay(userId, id, request);
    }
}
