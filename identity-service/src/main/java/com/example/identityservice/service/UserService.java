package com.example.identityservice.service;

import com.example.identityservice.dto.*;
import com.example.identityservice.exception.RegistrationException;
import com.example.identityservice.model.Organizer;
import com.example.identityservice.model.PasswordResetToken;
import com.example.identityservice.model.Role;
import com.example.identityservice.model.User;
import com.example.identityservice.repository.OrganizerRepository;
import com.example.identityservice.repository.PasswordResetTokenRepository;
import com.example.identityservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PaymentClient paymentClient;

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

    @Transactional
    public User registerOrganizer(OrganizerRegistrationDto registrationDto) {
        // Đăng ký user trước
        User user = registerUser(registrationDto);

        // Tạo và lưu thông tin organizer
        Organizer organizer = new Organizer();
        organizer.setUserId(user.getUserId());
        organizer.setOrganizationName(registrationDto.getOrganizationName());
        organizer.setContactEmail(registrationDto.getContactEmail());
        organizer.setDescription(registrationDto.getDescription());
        organizerRepository.save(organizer);

        return user;
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

    public Page<UserManagementResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToResponseDto);
    }

    public UserManagementResponseDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        return convertToResponseDto(user);
    }

    @Transactional
    public UserManagementResponseDto createUser(UserCreateDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RegistrationException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RegistrationException("Email đã tồn tại");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setActive(true);
        User savedUser = userRepository.save(user);
        return convertToResponseDto(savedUser);
    }

    @Transactional
    public UserManagementResponseDto updateUser(Integer id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new RegistrationException("Email đã tồn tại");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getFullName() != null)
            user.setFullName(dto.getFullName());
        if (dto.getPhone() != null)
            user.setPhone(dto.getPhone());
        if (dto.getAddress() != null)
            user.setAddress(dto.getAddress());
        if (dto.getRole() != null)
            user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        if (dto.getIsActive() != null)
            user.setActive(dto.getIsActive());
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        return convertToResponseDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        userRepository.delete(user);
    }

    @Transactional
    public void activateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        user.setActive(false);
        userRepository.save(user);
        sendDeactivationEmail(user.getEmail());
    }

    private void sendDeactivationEmail(String email) {
        try {
            emailService.sendDeactivationEmail(email);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email thông báo: " + e.getMessage());
        }
    }

    // Phương thức tìm kiếm theo ID với caching
    public Optional<User> findById(Integer userId) {
        String key = "user:id:" + userId;
        String cachedUser = redisTemplate.opsForValue().get(key);

        if (cachedUser != null) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                return user;
            }
        }

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            redisTemplate.opsForValue().set(key, "exists", 10, TimeUnit.MINUTES);
        }
        return user;
    }

    public UserTransactionHistory getUserTransactionHistory(Integer userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        ResponseWrapper<List<UserTransactionHistory.TransactionDetail>> wrapper = paymentClient
                .getTransactionsByUserId(userId, token);
        if (wrapper == null || !"success".equals(wrapper.getStatus())) {
            throw new RuntimeException("Không thể lấy lịch sử giao dịch");
        }

        UserTransactionHistory history = new UserTransactionHistory();
        history.setUserId(user.getUserId());
        history.setUsername(user.getUsername());
        history.setEmail(user.getEmail());
        history.setFullName(user.getFullName());
        history.setTransactions(wrapper.getData());

        return history;
    }

    public OrganizerProfileDto getOrganizerProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.ORGANIZER) {
            throw new IllegalStateException("User is not an organizer");
        }
        Organizer organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found"));

        OrganizerProfileDto dto = new OrganizerProfileDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setOrganizationName(organizer.getOrganizationName());
        dto.setContactEmail(organizer.getContactEmail());
        dto.setDescription(organizer.getDescription());
        return dto;
    }

    @Transactional
    public void updateOrganizerProfile(Integer userId, OrganizerUpdateDto updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.ORGANIZER) {
            throw new IllegalStateException("User is not an organizer");
        }
        Organizer organizer = organizerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found"));

        // Cập nhật thông tin user
        if (updateDto.getFullName() != null)
            user.setFullName(updateDto.getFullName());
        if (updateDto.getPhone() != null)
            user.setPhone(updateDto.getPhone());
        if (updateDto.getAddress() != null)
            user.setAddress(updateDto.getAddress());

        // Cập nhật thông tin organizer
        if (updateDto.getOrganizationName() != null)
            organizer.setOrganizationName(updateDto.getOrganizationName());
        if (updateDto.getContactEmail() != null)
            organizer.setContactEmail(updateDto.getContactEmail());
        if (updateDto.getDescription() != null)
            organizer.setDescription(updateDto.getDescription());

        userRepository.save(user);
        organizerRepository.save(organizer);
    }

    private UserManagementResponseDto convertToResponseDto(User user) {
        UserManagementResponseDto dto = new UserManagementResponseDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().toString());
        dto.setActive(user.isActive());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

}