package com.ebookstore.controller;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public CartDto get(@AuthenticationPrincipal Long userId) {
        return cartService.get(userId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Long userId) {
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public CartDto add(@AuthenticationPrincipal Long userId, @Valid @RequestBody CartItemRequest request) {
        return cartService.add(userId, request);
    }

    @PutMapping("/items/{productId}")
    public CartDto update(@AuthenticationPrincipal Long userId, @PathVariable Long productId,
                          @Valid @RequestBody CartItemRequest request) {
        return cartService.updateQuantity(userId, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartDto remove(@AuthenticationPrincipal Long userId, @PathVariable Long productId) {
        return cartService.remove(userId, productId);
    }
}
