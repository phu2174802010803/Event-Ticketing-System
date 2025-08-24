package com.example.identityservice.controller;

import com.example.identityservice.dto.*;
import com.example.identityservice.exception.AccountNotActivatedException;
import com.example.identityservice.exception.RegistrationException;
import com.example.identityservice.model.PasswordResetToken;
import com.example.identityservice.model.Role;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    private RestTemplate restTemplate;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

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
    public ResponseEntity<ResponseDto> registerOrganizer(@Valid @RequestBody OrganizerRegistrationDto organizerRegistrationDto) {
        organizerRegistrationDto.setRole("ORGANIZER"); // Mặc định là ORGANIZER cho Web
        User user = userService.registerOrganizer(organizerRegistrationDto);
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

    @PostMapping("/auth/google/login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            // Trao đổi mã ủy quyền để lấy access token
            String tokenUrl = "https://oauth2.googleapis.com/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "code=" + request.getCode() +
                    "&client_id=" + System.getenv("GOOGLE_CLIENT_ID") +
                    "&client_secret=" + System.getenv("GOOGLE_CLIENT_SECRET") +
                    "&redirect_uri=" + request.getRedirectUri() +
                    "&grant_type=authorization_code";
            HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseWrapper<>("error", "Không thể lấy access token từ Google", null));
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");

            // Lấy thông tin người dùng từ Google
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> userInfoEntity = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoEntity, Map.class);
            if (userInfoResponse.getStatusCode() != HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseWrapper<>("error", "Không thể lấy thông tin người dùng từ Google", null));
            }

            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");

            // Kiểm tra và tạo/tìm người dùng
            Optional<User> optionalUser = userRepository.findByEmail(email);
            User user;
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
            } else {
                user = new User();
                user.setUsername(email); // Dùng email làm username
                user.setEmail(email);
                user.setFullName(name);
                user.setRole(Role.USER);
                user.setActive(true);
                user = userRepository.save(user);
            }

            // Tạo JWT
            List<String> roles = Collections.singletonList(user.getRole().toString());
            String jwt = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);

            // Lưu thông tin vào Redis
            redisTemplate.opsForValue().set("user:google:" + user.getUserId(), email, 10, TimeUnit.MINUTES);

            return ResponseEntity.ok(new ResponseWrapper<>("success", "Đăng nhập bằng Google thành công",
                    new AuthenticationResponse(jwt, "Đăng nhập thành công", user.getUsername())));
        } catch (Exception e) {
            logger.error("Lỗi khi đăng nhập bằng Google: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseWrapper<>("error", "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        try {
            logger.info("Nhận callback từ Google với code: {}", code.substring(0, Math.min(code.length(), 20)) + "...");

            // Trao đổi mã ủy quyền để lấy access token
            String tokenUrl = "https://oauth2.googleapis.com/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String clientId = System.getenv("GOOGLE_CLIENT_ID");
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");

            // Fallback từ application.properties nếu env variables không có
            if (clientId == null || clientId.isEmpty()) {
                $1REDACTED$3;
            }
            if (clientSecret == null || clientSecret.isEmpty()) {
                $1REDACTED$3;
            }

            String body = "code=" + code +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&redirect_uri=http://localhost:8081/api/auth/google/callback" +
                    "&grant_type=authorization_code";

            HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Không thể lấy access token từ Google: {}", tokenResponse.getBody());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Lỗi xác thực với Google. Vui lòng thử lại.");
            }

            Map<String, Object> tokenData = tokenResponse.getBody();
            String accessToken = (String) tokenData.get("access_token");

            // Lấy thông tin người dùng từ Google
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> userInfoEntity = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoEntity,
                    Map.class);
            if (userInfoResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Không thể lấy thông tin người dùng từ Google: {}", userInfoResponse.getBody());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Không thể lấy thông tin người dùng từ Google.");
            }

            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String picture = (String) userInfo.get("picture");

            logger.info("Thông tin người dùng từ Google - Email: {}, Name: {}", email, name);

            // Kiểm tra và tạo/tìm người dùng
            Optional<User> optionalUser = userRepository.findByEmail(email);
            User user;
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                logger.info("Tìm thấy người dùng hiện có: {}", user.getUsername());
            } else {
                user = new User();
                user.setUsername(email); // Dùng email làm username
                user.setEmail(email);
                user.setFullName(name);
                user.setRole(Role.USER);
                user.setActive(true);
                user = userRepository.save(user);
                logger.info("Tạo người dùng mới: {}", user.getUsername());
            }

            // Tạo JWT
            List<String> roles = Collections.singletonList(user.getRole().toString());
            String jwt = jwtUtil.generateToken(user.getUserId(), user.getUsername(), roles);

            // Lưu thông tin vào Redis
            redisTemplate.opsForValue().set("user:google:" + user.getUserId(), email, 30, TimeUnit.MINUTES);

            // Redirect đến frontend với token (hoặc trả về HTML page với token)
            String redirectUrl = String.format("%s/auth/success?token=%s&username=%s", frontendUrl, jwt,
                    user.getUsername());

            // Trả về HTML redirect page
            String htmlResponse = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Đăng nhập thành công</title>
                        <script>
                            // Lưu token vào localStorage và redirect
                            localStorage.setItem('token', '%s');
                            localStorage.setItem('username', '%s');
                            window.location.href = '%s';
                        </script>
                    </head>
                    <body>
                        <p>Đăng nhập thành công! Đang chuyển hướng...</p>
                    </body>
                    </html>
                    """, jwt, user.getUsername(), frontendUrl);

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlResponse);

        } catch (Exception e) {
            logger.error("Lỗi khi xử lý callback Google: {}", e.getMessage(), e);
            String errorHtml = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Lỗi đăng nhập</title>
                    </head>
                    <body>
                        <h2>Lỗi đăng nhập với Google</h2>
                        <p>%s</p>
                        <a href="%s/login">Quay lại trang đăng nhập</a>
                    </body>
                    </html>
                    """, e.getMessage(), frontendUrl);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
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