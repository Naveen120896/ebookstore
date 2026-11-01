package com.ebookstore.service;

import com.ebookstore.dto.Dtos.CheckoutRequest;
import com.ebookstore.dto.Dtos.OrderDto;
import com.ebookstore.dto.Dtos.PaymentDto;
import com.ebookstore.dto.Dtos.PaymentRequest;
import com.ebookstore.entity.*;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepo;
    @Mock CartRepository cartRepo;
    @Mock AddressRepository addressRepo;
    @Mock UserRepository userRepo;
    @Mock PaymentRepository paymentRepo;
    @InjectMocks OrderService service;

    // ---------- helpers ----------
    private User user(int points) {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@b.com");
        u.setGiftPoints(points);
        return u;
    }

    private Product product(int stock) {
        Category c = new Category();
        c.setId(1L);
        c.setName("Tech");
        Product p = new Product();
        p.setId(10L);
        p.setTitle("Clean Code");
        p.setPrice(new BigDecimal("500.00"));
        p.setStock(stock);
        p.setDeliveryDays(3);
        p.setCategory(c);
        return p;
    }

    private Cart cartWith(User u, Product p, int qty) {
        Cart cart = new Cart();
        cart.setUser(u);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(p);
        item.setQuantity(qty);
        cart.getItems().add(item);
        return cart;
    }

    private Address address(User u) {
        Address a = new Address();
        a.setId(5L);
        a.setUser(u);
        return a;
    }

    private Order order(User u, Product p, int qty, OrderStatus status, String total, int pointsUsed, LocalDateTime createdAt) {
        Order o = new Order();
        o.setId(9L);
        o.setUser(u);
        o.setStatus(status);
        o.setSubtotal(new BigDecimal("1000.00"));
        o.setGiftPointsUsed(pointsUsed);
        o.setTotal(new BigDecimal(total));
        o.setCreatedAt(createdAt);
        OrderItem oi = new OrderItem();
        oi.setOrder(o);
        oi.setProduct(p);
        oi.setQuantity(qty);
        oi.setUnitPrice(p.getPrice());
        o.getItems().add(oi);
        return o;
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ---------- checkout ----------
    @Test
    void checkout_success_reducesStock_deductsPoints_clearsCart() {
        User u = user(100);
        Product p = product(10);
        Cart cart = cartWith(u, p, 2);
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address(u)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));

        OrderDto dto = service.checkout(1L, new CheckoutRequest(5L, 50));

        assertEquals("PLACED", dto.status());
        assertAmount("1000", dto.subtotal());
        assertEquals(50, dto.giftPointsUsed());
        assertAmount("950", dto.total());
        assertEquals(8, p.getStock(), "stock should drop by the ordered quantity");
        assertEquals(50, u.getGiftPoints(), "points should be deducted");
        assertTrue(cart.getItems().isEmpty(), "cart should be emptied");
    }

    @Test
    void checkout_emptyCart_isRejected() {
        User u = user(100);
        Cart empty = new Cart();
        empty.setUser(u);
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(empty));

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(1L, new CheckoutRequest(5L, 0)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void checkout_insufficientStock_isRejected() {
        User u = user(100);
        Product p = product(1);           // only 1 in stock
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cartWith(u, p, 2)));
        when(addressRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address(u)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(1L, new CheckoutRequest(5L, 0)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void checkout_moreGiftPointsThanOwned_isRejected() {
        User u = user(100);               // owns 100 points
        Product p = product(10);
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cartWith(u, p, 2)));
        when(addressRepo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address(u)));
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));

        ApiException ex = assertThrows(ApiException.class, () -> service.checkout(1L, new CheckoutRequest(5L, 150)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ---------- cancel (48 hour rule) ----------
    @Test
    void cancel_within48Hours_restoresStockAndPoints_andRefundsPayment() {
        User u = user(50);
        Product p = product(8);
        Order o = order(u, p, 2, OrderStatus.PAID, "950.00", 50, LocalDateTime.now().minusHours(10));
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));
        when(paymentRepo.findByOrderId(9L)).thenReturn(Optional.of(payment));

        OrderDto dto = service.cancel(1L, 9L);

        assertEquals("CANCELLED", dto.status());
        assertEquals(10, p.getStock(), "stock should be restored");
        assertEquals(100, u.getGiftPoints(), "gift points should be refunded");
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertNotNull(o.getCancelledAt());
    }

    @Test
    void cancel_after48Hours_isRejected() {
        User u = user(50);
        Product p = product(8);
        Order o = order(u, p, 2, OrderStatus.PAID, "950.00", 50, LocalDateTime.now().minusHours(49));
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(1L, 9L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(OrderStatus.PAID, o.getStatus(), "order must stay untouched");
        assertEquals(8, p.getStock());
    }

    @Test
    void cancel_alreadyCancelled_isRejected() {
        Order o = order(user(0), product(8), 2, OrderStatus.CANCELLED, "950.00", 0, LocalDateTime.now().minusHours(1));
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(1L, 9L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void cancel_unknownOrder_returnsNotFound() {
        when(orderRepo.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ---------- payment ----------
    @Test
    void pay_validCard_marksOrderPaid_andStoresOnlyLast4() {
        Order o = order(user(50), product(8), 2, OrderStatus.PLACED, "950.00", 50, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        PaymentDto dto = service.pay(1L, 9L,
                new PaymentRequest("CREDIT_CARD", "4111 1111 1111 1111", "Naveen", "12/28", "123"));

        assertEquals("SUCCESS", dto.status());
        assertEquals("Your purchase is complete", dto.message());
        assertEquals(OrderStatus.PAID, o.getStatus());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepo).save(captor.capture());
        assertEquals("1111", captor.getValue().getCardLast4());
        assertEquals(PaymentMethod.CREDIT_CARD, captor.getValue().getMethod());
    }

    @Test
    void pay_cardEndingWith0000_isDeclined() {
        Order o = order(user(50), product(8), 2, OrderStatus.PLACED, "950.00", 50, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.pay(1L, 9L,
                new PaymentRequest("CREDIT_CARD", "4111111111110000", "Naveen", "12/28", "123")));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals(OrderStatus.PLACED, o.getStatus());
        verify(paymentRepo, never()).save(any(Payment.class));
    }

    @Test
    void pay_invalidCardNumber_isRejected() {
        Order o = order(user(50), product(8), 2, OrderStatus.PLACED, "950.00", 50, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.pay(1L, 9L,
                new PaymentRequest("DEBIT_CARD", "123", "Naveen", "12/28", "123")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void pay_giftPointsMethod_whenAmountStillDue_isRejected() {
        Order o = order(user(50), product(8), 2, OrderStatus.PLACED, "950.00", 50, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.pay(1L, 9L,
                new PaymentRequest("GIFT_POINTS", null, null, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void pay_whenGiftPointsCoverEverything_needsNoCard() {
        Order o = order(user(0), product(8), 2, OrderStatus.PLACED, "0.00", 1000, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        PaymentDto dto = service.pay(1L, 9L, new PaymentRequest("GIFT_POINTS", null, null, null, null));

        assertEquals("GIFT_POINTS", dto.method());
        assertEquals(OrderStatus.PAID, o.getStatus());
    }

    @Test
    void pay_alreadyPaidOrder_isRejected() {
        Order o = order(user(50), product(8), 2, OrderStatus.PAID, "950.00", 50, LocalDateTime.now());
        when(orderRepo.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(o));

        ApiException ex = assertThrows(ApiException.class, () -> service.pay(1L, 9L,
                new PaymentRequest("CREDIT_CARD", "4111111111111111", "Naveen", "12/28", "123")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}