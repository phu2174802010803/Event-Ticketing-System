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
import com.example.identityservice.exception.AccountNotApprovedException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<ResponseDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        registrationDto.setRole("USER"); // Mặc định là USER cho App
        User user = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(user.getUserId(), "Đăng ký người dùng thành công"));
    }

    @PostMapping("/organizer/register")
    public ResponseEntity<ResponseDto> registerOrganizer(@Valid @RequestBody UserRegistrationDto registrationDto) {
        registrationDto.setRole("ORGANIZER"); // Mặc định là ORGANIZER cho Web
        User user = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(user.getUserId(), "Đăng ký Organizer thành công, chờ phê duyệt"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        logger.info("Nhận yêu cầu đăng nhập cho username: {}", loginDto.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
            );
            final UserDetails userDetails = userDetailsService.loadUserByUsername(loginDto.getUsername());
            final String jwt = jwtUtil.generateToken(userDetails.getUsername());
            return ResponseEntity.ok(new AuthenticationResponse(jwt, "Đăng nhập thành công", loginDto.getUsername()));
        } catch (BadCredentialsException e) {
            logger.error("Sai thông tin đăng nhập: {}", loginDto.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Sai tên đăng nhập hoặc mật khẩu"));
        } catch (AccountNotActivatedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Tài khoản chưa được kích hoạt"));
        } catch (AccountNotApprovedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Tài khoản chưa được phê duyệt"));
        }
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token, userDetails)) {
                return ResponseEntity.ok(userDetails.getAuthorities().iterator().next().getAuthority());
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
        String result = userService.validatePasswordResetToken(request.getToken());
        if ("valid".equals(result)) {
            return ResponseEntity.ok("Mã xác nhận hợp lệ");
        } else if ("expired".equals(result)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Mã xác nhận đã hết hạn"));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Mã xác nhận không hợp lệ"));
        }
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