package com.ebookstore.service;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.dto.Mapper;
import com.ebookstore.entity.Cart;
import com.ebookstore.entity.CartItem;
import com.ebookstore.entity.Product;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.CartRepository;
import com.ebookstore.repository.ProductRepository;
import com.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public CartDto get(Long userId) {
        return Mapper.cart(cartFor(userId));
    }

    public CartDto add(Long userId, CartItemRequest req) {
        Cart cart = cartFor(userId);
        Product p = product(req.productId());
        CartItem item = findItem(cart, p.getId());
        int newQty = (item == null ? 0 : item.getQuantity()) + req.quantity();
        checkStock(p, newQty);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(p);
            cart.getItems().add(item);
        }
        item.setQuantity(newQty);
        return Mapper.cart(cart);
    }

    public CartDto updateQuantity(Long userId, Long productId, CartItemRequest req) {
        Cart cart = cartFor(userId);
        CartItem item = findItem(cart, productId);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Item not in cart");
        }
        checkStock(item.getProduct(), req.quantity());
        item.setQuantity(req.quantity());
        return Mapper.cart(cart);
    }

    public CartDto remove(Long userId, Long productId) {
        Cart cart = cartFor(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return Mapper.cart(cart);
    }

    public void clear(Long userId) {
        cartFor(userId).getItems().clear();
    }

    // ---- helpers ----
    private Cart cartFor(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(userRepo.getReferenceById(userId));
            return cartRepo.save(c);
        });
    }

    private Product product(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElse(null);
    }

    private void checkStock(Product p, int qty) {
        if (qty > p.getStock()) {
            throw new ApiException(HttpStatus.CONFLICT, "Only " + p.getStock() + " copies of '" + p.getTitle() + "' in stock");
        }
    }
}
