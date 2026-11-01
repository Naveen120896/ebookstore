package com.ebookstore.controller;

import com.ebookstore.dto.Dtos.AddressDto;
import com.ebookstore.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping
    public List<AddressDto> list(@AuthenticationPrincipal Long userId) {
        return addressService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto create(@AuthenticationPrincipal Long userId, @Valid @RequestBody AddressDto address) {
        return addressService.create(userId, address);
    }
}
