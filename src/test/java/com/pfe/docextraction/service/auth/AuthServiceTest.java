package com.pfe.docextraction.service.auth;

import com.pfe.docextraction.dto.RegisterRequest;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.enums.UserRole;
import com.pfe.docextraction.repository.UserRepository;
import com.pfe.docextraction.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Captor
    ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setup() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    @Test
    void register_withoutRole_assignsLecteur() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password");
        req.setRole(null);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNotNull(saved);
        assertEquals(UserRole.LECTEUR, saved.getRole());
    }

    @Test
    void register_withOperateur_assignsOperateur() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("opuser");
        req.setEmail("op@example.com");
        req.setPassword("password");
        req.setRole("OPERATEUR");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNotNull(saved);
        assertEquals(UserRole.OPERATEUR, saved.getRole());
    }
}
