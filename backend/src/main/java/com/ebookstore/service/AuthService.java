package com.ebookstore.service;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.dto.Mapper;
import com.ebookstore.entity.User;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.UserRepository;
import com.ebookstore.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int WELCOME_POINTS = 100;

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @Transactional
    public UserDto register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName());
        u.setGiftPoints(WELCOME_POINTS); // welcome gift points so redemption can be demoed
        return Mapper.user(userRepo.save(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = userRepo.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return new AuthResponse(jwtService.generate(u), Mapper.user(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return userRepo.findById(userId)
                .map(Mapper::user)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}
