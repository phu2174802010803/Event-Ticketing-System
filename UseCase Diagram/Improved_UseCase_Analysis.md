# PHÂN TÍCH VÀ CẢI TIẾN USECASE DIAGRAM - HỆ THỐNG EVENT TICKETS

## 1. VẤN ĐỀ VỚI USECASE DIAGRAM HIỆN TẠI

### 1.1. Thiếu System Boundary
- Không có boundary rõ ràng cho từng microservice
- Không phân biệt được domain của các chức năng
- Thiếu phân cách giữa internal và external systems

### 1.2. Sai lầm trong mối quan hệ Include/Extend
- Sử dụng `<<extend>>` khi nên dùng `<<include>>`
- Thiếu mối quan hệ phụ thuộc giữa các use case
- Không thể hiện được luồng nghiệp vụ chính

### 1.3. Thiếu External Actors
- Không có Guest actor cho public APIs
- Thiếu External Systems (VNPay, Email Service, Storage)
- Không thể hiện third-party integrations

### 1.4. Phân nhóm không logic
- Các use case không được nhóm theo domain
- Thiếu tính tổ chức theo kiến trúc microservices

## 2. PHÂN TÍCH CHỨC NĂNG THỰC TẾ TỪ MÃ NGUỒN

### 2.1. ADMIN (Quản trị viên)
**Identity Service:**
- Quản lý người dùng (CRUD, activate/deactivate)
- Xem lịch sử giao dịch của người dùng

**Event Service:**
- Quản lý sự kiện (CRUD, approve/reject)
- Quản lý danh mục sự kiện
- Quản lý phiên bán vé

**Payment Service:**
- Xem tất cả giao dịch
- Tạo báo cáo tài chính
- Thống kê giao dịch

**Ticket Service:**
- Quản lý vé toàn hệ thống

### 2.2. ORGANIZER (Nhà tổ chức)
**Identity Service:**
- Quản lý hồ sơ cá nhân

**Event Service:**
- Quản lý sự kiện của mình (CRUD)
- Upload hình ảnh/banner
- Quản lý phiên bán vé

**Payment Service:**
- Xem giao dịch của sự kiện mình

**Ticket Service:**
- Quản lý vé của sự kiện mình

### 2.3. USER (Người dùng)
**Identity Service:**
- Xác thực (đăng ký, đăng nhập, đăng xuất)
- Quên/đặt lại mật khẩu
- Đăng nhập Google
- Quản lý hồ sơ cá nhân

**Event Service (Public):**
- Xem danh sách sự kiện
- Tìm kiếm/lọc sự kiện
- Xem chi tiết sự kiện

**Ticket Service:**
- Chọn vé (booking tạm thời)
- Mua vé
- Xem vé của mình
- Xem mã QR vé

**Payment Service:**
- Xử lý thanh toán
- Xem lịch sử giao dịch cá nhân

### 2.4. GUEST (Khách)
**Event Service (Public):**
- Xem danh sách sự kiện công khai
- Tìm kiếm sự kiện
- Lọc sự kiện
- Xem chi tiết sự kiện

## 3. KIẾN TRÚC MICROSERVICES VÀ BOUNDARY

### 3.1. Identity Service
```
Boundary: User Management & Authentication
- Authentication Domain
- User Profile Management Domain
- Authorization Domain
```

### 3.2. Event Service
```
Boundary: Event Management
- Event Management Domain
- Category Management Domain
- Public Event Access Domain
```

### 3.3. Payment Service
```
Boundary: Payment Processing
- Payment Management Domain
- Transaction Management Domain
- Financial Reporting Domain
```

### 3.4. Ticket Service
```
Boundary: Ticket Management
- Ticket Booking Domain
- Ticket Purchase Domain
- Ticket Validation Domain
```

### 3.5. Notification Service
```
Boundary: Communication
- Email Notification Domain
- Push Notification Domain
```

## 4. EXTERNAL SYSTEMS

### 4.1. VNPay Integration
- Payment Gateway cho việc thanh toán
- Actor: Secondary Actor
- Relationship: `<<include>>` với "Xử lý thanh toán"

### 4.2. Email Service (SendGrid)
- Gửi email xác thực, reset password
- Actor: Secondary Actor
- Relationship: `<<include>>` với "Quên mật khẩu", "Đăng ký"

### 4.3. Storage Service (DigitalOcean Spaces)
- Lưu trữ hình ảnh/banner sự kiện
- Actor: Secondary Actor
- Relationship: `<<include>>` với "Tạo sự kiện", "Cập nhật sự kiện"

### 4.4. Google OAuth
- Đăng nhập bằng Google
- Actor: Secondary Actor
- Relationship: `<<include>>` với "Đăng nhập Google"

## 5. MỐI QUAN HỆ INCLUDE/EXTEND ĐÚNG

### 5.1. Include Relationships (Bắt buộc)
```
"Đăng ký" <<include>> "Gửi email xác thực"
"Quên mật khẩu" <<include>> "Gửi email reset"
"Tạo sự kiện" <<include>> "Upload hình ảnh"
"Mua vé" <<include>> "Xử lý thanh toán"
"Đăng nhập Google" <<include>> "OAuth validation"
```

### 5.2. Extend Relationships (Tùy chọn)
```
"Xem chi tiết sự kiện" <<extend>> "Đăng ký tham gia"
"Xem danh sách sự kiện" <<extend>> "Lọc theo danh mục"
"Xem danh sách sự kiện" <<extend>> "Tìm kiếm"
"Quản lý sự kiện" <<extend>> "Phê duyệt sự kiện" (cho Admin)
```

## 6. ĐỀ XUẤT CẢI TIẾN

### 6.1. Cấu trúc UseCase Diagram mới
1. **Tạo System Boundary cho từng service**
2. **Phân nhóm theo Domain**
3. **Thêm External Actors/Systems**
4. **Sử dụng đúng Include/Extend**
5. **Phân biệt Primary/Secondary Actors**

### 6.2. Nguyên tắc thiết kế
1. **Một diagram cho một Actor chính**
2. **Boundary rõ ràng cho từng service**
3. **Include cho dependencies bắt buộc**
4. **Extend cho optional features**
5. **External systems như Secondary Actors**

### 6.3. Tools và Standards
- Sử dụng UML 2.5 standard
- Boundary notation: `<<subsystem>>`
- Actor notation: `<<actor>>`
- External system: `<<external>>`

## 7. KẾT LUẬN

UseCase Diagram hiện tại cần được cải tiến để:
1. Phản ánh đúng kiến trúc microservices
2. Thể hiện rõ boundary và domain
3. Sử dụng đúng UML relationships
4. Bao gồm external systems
5. Phân biệt rõ vai trò của từng actor

Việc cải tiến này sẽ giúp:
- Hiểu rõ hơn về kiến trúc hệ thống
- Dễ dàng maintain và extend
- Chuẩn hóa theo UML standards
- Hỗ trợ tốt cho việc phát triển và testing 