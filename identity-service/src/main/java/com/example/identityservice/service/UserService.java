package com.example.identityservice.service;

import com.example.identityservice.dto.UserRegistrationDto;
import com.example.identityservice.exception.RegistrationException;
import com.example.identityservice.model.PasswordResetToken;
import com.example.identityservice.model.Role;
import com.example.identityservice.model.User;
import com.example.identityservice.repository.PasswordResetTokenRepository;
import com.example.identityservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Thêm RedisTemplate

    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RegistrationException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RegistrationException("Email đã tồn tại");
        }

        String roleStr = registrationDto.getRole().toUpperCase();
        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new RegistrationException("Vai trò không hợp lệ");
        }

        if (role == Role.ADMIN) {
            throw new RegistrationException("Không thể đăng ký với vai trò ADMIN");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(role);
        user.setEmail(registrationDto.getEmail());
        user.setFullName(registrationDto.getFullName());
        user.setPhone(registrationDto.getPhone());
        user.setAddress(registrationDto.getAddress());
        user.setActive(true);

        return userRepository.save(user);
    }

    // Phương thức tìm kiếm với caching
    public Optional<User> findByUsername(String username) {
        String key = "user:username:" + username;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId != null) {
            Optional<User> cachedUser = userRepository.findById(Integer.parseInt(userId));
            if (cachedUser.isPresent()) {
                return cachedUser;
            }
        }

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            redisTemplate.opsForValue().set(key, user.get().getUserId().toString(), 10, TimeUnit.MINUTES);
        }
        return user;
    }

    // Phương thức tìm kiếm với caching
    public Optional<User> findByEmail(String email) {
        String key = "user:email:" + email;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId != null) {
            Optional<User> cachedUser = userRepository.findById(Integer.parseInt(userId));
            if (cachedUser.isPresent()) {
                return cachedUser;
            }
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            redisTemplate.opsForValue().set(key, user.get().getUserId().toString(), 10, TimeUnit.MINUTES);
        }
        return user;
    }

    // Cập nhật thông tin người dùng và xóa cache
    @Transactional
    public User save(User user) {
        User savedUser = userRepository.save(user);
        String key = "user:username:" + user.getUsername();
        redisTemplate.delete(key); // Xóa cache khi thông tin thay đổi
        return savedUser;
    }

    @Transactional
    public String createPasswordResetTokenForUser(User user) {
        passwordResetTokenRepository.deleteByUserId(user.getUserId());
        String token = String.format("%06d", random.nextInt(1000000));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getUserId());
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passToken = passwordResetTokenRepository.findByToken(token);
        if (passToken.isEmpty()) {
            return "invalid";
        }
        if (passToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            return "expired";
        }
        return "valid";
    }

    @Transactional
    public void changeUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUserId(user.getUserId());
        String key = "user:username:" + user.getUsername();
        redisTemplate.delete(key); // Xóa cache khi mật khẩu thay đổi
    }


}