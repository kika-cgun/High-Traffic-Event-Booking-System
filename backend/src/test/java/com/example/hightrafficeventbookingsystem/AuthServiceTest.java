package com.example.hightrafficeventbookingsystem;

import com.example.hightrafficeventbookingsystem.dto.AuthResponse;
import com.example.hightrafficeventbookingsystem.dto.RefreshTokenRequest;
import com.example.hightrafficeventbookingsystem.model.Role;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.repository.UserRepository;
import com.example.hightrafficeventbookingsystem.security.JwtService;
import com.example.hightrafficeventbookingsystem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    AuthService authService;

    @Test
    void refresh_returnsNewTokenPairWhenRefreshTokenIsValid() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setRole(Role.USER);

        when(jwtService.extractUsername("refresh-token-old")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isRefreshTokenValid("refresh-token-old", user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-new");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token-new");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("refresh-token-old"));

        assertThat(response.token()).isEqualTo("access-token-new");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-new");
    }

    @Test
    void refresh_throwsUnauthorizedWhenRefreshTokenIsInvalid() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(jwtService.extractUsername("bad-refresh-token")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isRefreshTokenValid("bad-refresh-token", user)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("bad-refresh-token")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED")
                .hasMessageContaining("Invalid or expired refresh token");
    }
}