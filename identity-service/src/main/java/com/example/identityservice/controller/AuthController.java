package com.example.identityservice.controller;

import com.example.identityservice.dto.AuthenticationResponse;
import com.example.identityservice.dto.ErrorResponse;
import com.example.identityservice.dto.ForgotPasswordRequest;
import com.example.identityservice.dto.LoginDto;
import com.example.identityservice.dto.ResetPasswordRequest;
import com.example.identityservice.dto.ResponseDto;
import com.example.identityservice.dto.UserRegistrationDto;
import com.example.identityservice.dto.VerifyCodeRequest;
import com.example.identityservice.exception.AccountNotActivatedException;
import com.example.identityservice.exception.RegistrationException;
import com.example.identityservice.model.PasswordResetToken;
import com.example.identityservice.model.User;
import com.example.identityservice.repository.PasswordResetTokenRepository;
import com.example.identityservice.repository.UserRepository;
import com.example.identityservice.service.EmailService;
import com.example.identityservice.service.UserService;
import com.example.identityservice.util.JwtUtil;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            registrationDto.setRole("USER"); // Mặc định là USER cho App
            User user = userService.registerUser(registrationDto);
            return ResponseEntity.ok(new ResponseDto(user.getUserId(), "Đăng ký thành công"));
        } catch (RegistrationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/organizer/register")
    public ResponseEntity<ResponseDto> registerOrganizer(@Valid @RequestBody UserRegistrationDto registrationDto) {
        registrationDto.setRole("ORGANIZER"); // Mặc định là ORGANIZER cho Web
        User user = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(user.getUserId(), "Đăng ký Organizer thành công"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        logger.info("Nhận yêu cầu đăng nhập cho login: {}", loginDto.getLogin());
        try {
            User user = userRepository.findByUsername(loginDto.getLogin())
                    .or(() -> userRepository.findByEmail(loginDto.getLogin()))
                    .orElseThrow(() -> new BadCredentialsException("Không tìm thấy người dùng với thông tin đăng nhập: " + loginDto.getLogin()));

            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), loginDto.getPassword())
            );

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList());

            // Tạo token với cả userId và username
            final String jwt = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);
            return ResponseEntity.ok(new AuthenticationResponse(jwt, "Đăng nhập thành công", user.getUsername()));
        } catch (BadCredentialsException e) {
            logger.error("Sai thông tin đăng nhập: {}", loginDto.getLogin(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Sai tên đăng nhập/email hoặc mật khẩu"));
        } catch (AccountNotActivatedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Tài khoản chưa được kích hoạt"));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            long expiry = jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (expiry > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "revoked", expiry, TimeUnit.MILLISECONDS);
            }
            return ResponseEntity.ok(new ResponseDto(null, "Đăng xuất thành công"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Token không hợp lệ"));
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token, userDetails)) {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                String userInfo = user.getUserId() + ":" + user.getRole().toString();
                return ResponseEntity.ok(userInfo); // Trả về "user_id:role", ví dụ "1:USER"
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        String token;
        try {
            token = userService.createPasswordResetTokenForUser(user);
            emailService.sendPasswordResetEmail(request.getEmail(), token);
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Không thể gửi email: " + e.getMessage()));
        }
        return ResponseEntity.ok(new ResponseDto(user.getUserId(), "Mã xác nhận đã được gửi qua email"));
    }

    @PostMapping("/auth/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Mã xác nhận không tồn tại"));

        if (!token.getUserId().equals(user.getUserId())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Mã xác nhận không hợp lệ cho email này"));
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Mã xác nhận đã hết hạn"));
        }

        return ResponseEntity.ok(new ResponseDto(user.getUserId(), "Mã xác nhận hợp lệ"));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String result = userService.validatePasswordResetToken(request.getToken());
        if (!"valid".equals(result)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Mã xác nhận không hợp lệ hoặc đã hết hạn"));
        }

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Mã xác nhận không tồn tại"));
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        userService.changeUserPassword(user, request.getPassword());
        return ResponseEntity.ok(new ResponseDto(user.getUserId(), "Đặt lại mật khẩu thành công"));
    }
}