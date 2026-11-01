package com.ebookstore.service;

import com.ebookstore.dto.Dtos.CartDto;
import com.ebookstore.dto.Dtos.CartItemRequest;
import com.ebookstore.entity.Cart;
import com.ebookstore.entity.Category;
import com.ebookstore.entity.Product;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.CartRepository;
import com.ebookstore.repository.ProductRepository;
import com.ebookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepo;
    @Mock ProductRepository productRepo;
    @Mock UserRepository userRepo;
    @InjectMocks CartService service;

    private Product product(int stock) {
        Category c = new Category();
        c.setId(1L);
        c.setName("Tech");
        Product p = new Product();
        p.setId(10L);
        p.setTitle("Clean Code");
        p.setPrice(new BigDecimal("500.00"));
        p.setStock(stock);
        p.setCategory(c);
        return p;
    }

    @Test
    void add_newProduct_createsCartLine_andCalculatesSubtotal() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(new Cart()));
        when(productRepo.findById(10L)).thenReturn(Optional.of(product(5)));

        CartDto cart = service.add(1L, new CartItemRequest(10L, 2));

        assertEquals(1, cart.items().size());
        assertEquals(2, cart.items().get(0).quantity());
        assertEquals(0, new BigDecimal("1000").compareTo(cart.subtotal()));
    }

    @Test
    void add_sameProductTwice_mergesQuantity() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(new Cart()));
        when(productRepo.findById(10L)).thenReturn(Optional.of(product(5)));

        service.add(1L, new CartItemRequest(10L, 2));
        CartDto cart = service.add(1L, new CartItemRequest(10L, 1));

        assertEquals(1, cart.items().size(), "should stay one line");
        assertEquals(3, cart.items().get(0).quantity());
    }

    @Test
    void add_moreThanStock_isRejected() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(new Cart()));
        when(productRepo.findById(10L)).thenReturn(Optional.of(product(5)));

        ApiException ex = assertThrows(ApiException.class, () -> service.add(1L, new CartItemRequest(10L, 6)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void add_unknownProduct_returnsNotFound() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(new Cart()));
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.add(1L, new CartItemRequest(99L, 1)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void remove_deletesTheLine() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(new Cart()));
        when(productRepo.findById(10L)).thenReturn(Optional.of(product(5)));
        service.add(1L, new CartItemRequest(10L, 2));

        CartDto cart = service.remove(1L, 10L);

        assertTrue(cart.items().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.subtotal()));
    }
}