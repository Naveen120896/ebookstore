package com.ebookstore.service;

import com.ebookstore.dto.Dtos.AuthResponse;
import com.ebookstore.dto.Dtos.LoginRequest;
import com.ebookstore.dto.Dtos.RegisterRequest;
import com.ebookstore.dto.Dtos.UserDto;
import com.ebookstore.entity.User;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.UserRepository;
import com.ebookstore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService service;

    private User existingUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@b.com");
        u.setPasswordHash("HASH");
        u.setFullName("Naveen");
        u.setGiftPoints(100);
        return u;
    }

    @Test
    void register_newUser_hashesPassword_lowercasesEmail_givesWelcomePoints() {
        when(userRepo.existsByEmail("a@b.com")).thenReturn(false);
        when(encoder.encode("password1")).thenReturn("HASH");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = service.register(new RegisterRequest("A@B.com", "password1", "Naveen"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("a@b.com", saved.getEmail());
        assertEquals("HASH", saved.getPasswordHash(), "password must be stored hashed, never plain");
        assertEquals(100, saved.getGiftPoints());
        assertEquals(100, dto.giftPoints());
    }

    @Test
    void register_duplicateEmail_isRejected() {
        when(userRepo.existsByEmail("a@b.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(new RegisterRequest("a@b.com", "password1", "Naveen")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void login_correctPassword_returnsToken() {
        User u = existingUser();
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(encoder.matches("password1", "HASH")).thenReturn(true);
        when(jwtService.generate(u)).thenReturn("TOKEN");

        AuthResponse res = service.login(new LoginRequest("a@b.com", "password1"));

        assertEquals("TOKEN", res.token());
        assertEquals("a@b.com", res.user().email());
    }

    @Test
    void login_wrongPassword_isUnauthorized() {
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches("wrong", "HASH")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("a@b.com", "wrong")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_unknownEmail_isUnauthorized() {
        when(userRepo.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("nobody@b.com", "password1")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}