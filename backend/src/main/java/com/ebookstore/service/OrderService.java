package com.ebookstore.service;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.dto.Mapper;
import com.ebookstore.entity.*;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final long CANCEL_WINDOW_HOURS = 48;

    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final AddressRepository addressRepo;
    private final UserRepository userRepo;
    private final PaymentRepository paymentRepo;

    @Transactional(readOnly = true)
    public List<OrderDto> history(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(Mapper::order).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long userId, Long orderId) {
        return Mapper.order(find(userId, orderId));
    }

    /** Turns the cart into an order: checks stock, redeems gift points, empties the cart. */
    @Transactional
    public OrderDto checkout(Long userId, CheckoutRequest req) {
        Cart cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }
        Address address = addressRepo.findByIdAndUserId(req.addressId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid delivery address"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            if (p.getStock() < ci.getQuantity()) {
                throw new ApiException(HttpStatus.CONFLICT, "Insufficient stock for '" + p.getTitle() + "'");
            }
            p.setStock(p.getStock() - ci.getQuantity());

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(p.getPrice()); // price snapshot
            oi.setExpectedDelivery(LocalDate.now().plusDays(p.getDeliveryDays()));
            order.getItems().add(oi);
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // 1 gift point = 1 currency unit
        int points = req.giftPointsToRedeem() == null ? 0 : req.giftPointsToRedeem();
        if (points < 0 || points > user.getGiftPoints() || BigDecimal.valueOf(points).compareTo(subtotal) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid gift points amount");
        }
        user.setGiftPoints(user.getGiftPoints() - points);

        order.setSubtotal(subtotal);
        order.setGiftPointsUsed(points);
        order.setTotal(subtotal.subtract(BigDecimal.valueOf(points)));
        order.setStatus(OrderStatus.PLACED);
        orderRepo.save(order);

        cart.getItems().clear();
        return Mapper.order(order);
    }

    /** Pays for a PLACED order. Demo gateway: card numbers ending in 0000 are declined. */
    @Transactional
    public PaymentDto pay(Long userId, Long orderId, PaymentRequest req) {
        Order order = find(userId, orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is not awaiting payment (status " + order.getStatus() + ")");
        }
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(req.method().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown payment method");
        }

        String last4 = null;
        boolean fullyCoveredByPoints = order.getTotal().compareTo(BigDecimal.ZERO) == 0;
        if (fullyCoveredByPoints) {
            method = PaymentMethod.GIFT_POINTS;
        } else {
            if (method == PaymentMethod.GIFT_POINTS) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Gift points are redeemed at checkout; pay the remaining amount by card");
            }
            String digits = req.cardNumber() == null ? "" : req.cardNumber().replaceAll("\\s|-", "");
            if (!digits.matches("\\d{12,19}") || req.cvv() == null || !req.cvv().matches("\\d{3,4}")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid card details");
            }
            if (digits.endsWith("0000")) {
                throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment declined by the bank");
            }
            last4 = digits.substring(digits.length() - 4);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(order.getTotal());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCardLast4(last4);
        paymentRepo.save(payment);

        order.setStatus(OrderStatus.PAID);
        return Mapper.payment(payment, "Your purchase is complete");
    }

    /** Cancels within 48 hours of placing; restores stock and gift points, refunds payment. */
    @Transactional
    public OrderDto cancel(Long userId, Long orderId) {
        Order order = find(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is already " + order.getStatus());
        }
        if (LocalDateTime.now().isAfter(order.getCreatedAt().plusHours(CANCEL_WINDOW_HOURS))) {
            throw new ApiException(HttpStatus.CONFLICT, "Cancellation window (48 hours) has passed");
        }
        for (OrderItem oi : order.getItems()) {
            Product p = oi.getProduct();
            p.setStock(p.getStock() + oi.getQuantity());
        }
        User user = order.getUser();
        user.setGiftPoints(user.getGiftPoints() + order.getGiftPointsUsed());
        paymentRepo.findByOrderId(orderId).ifPresent(p -> p.setStatus(PaymentStatus.REFUNDED));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        return Mapper.order(order);
    }

    private Order find(Long userId, Long orderId) {
        return orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
