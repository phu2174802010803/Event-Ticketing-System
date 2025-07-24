# TEMPLATE USECASE DIAGRAM CẢI TIẾN

## 1. USECASE DIAGRAM CHO ADMIN

### Structure:
```
System Boundary: Event Ticket Management System
  |
  ├── Identity Service Boundary
  │   ├── User Management Domain
  │   │   ├── UC: Quản lý người dùng
  │   │   ├── UC: Kích hoạt tài khoản
  │   │   └── UC: Vô hiệu hóa tài khoản
  │   └── Authentication Domain
  │       └── UC: Xem lịch sử đăng nhập
  │
  ├── Event Service Boundary
  │   ├── Event Management Domain
  │   │   ├── UC: Phê duyệt sự kiện
  │   │   ├── UC: Từ chối sự kiện
  │   │   └── UC: Xóa sự kiện
  │   └── Category Management Domain
  │       ├── UC: Tạo danh mục
  │       ├── UC: Cập nhật danh mục
  │       └── UC: Xóa danh mục
  │
  ├── Payment Service Boundary
  │   └── Financial Management Domain
  │       ├── UC: Xem tất cả giao dịch
  │       ├── UC: Tạo báo cáo tài chính
  │       └── UC: Thống kê doanh thu
  │
  └── Ticket Service Boundary
      └── Ticket Management Domain
          ├── UC: Xem tất cả vé
          └── UC: Hủy vé
```

### Actors:
- **Primary Actor**: Admin
- **Secondary Actors**: Email Service (cho thông báo), Database System

### Include/Extend Relationships:
```
"Tạo báo cáo tài chính" <<include>> "Truy xuất dữ liệu giao dịch"
"Phê duyệt sự kiện" <<include>> "Gửi thông báo email"
"Vô hiệu hóa tài khoản" <<include>> "Gửi email thông báo"
```

## 2. USECASE DIAGRAM CHO ORGANIZER

### Structure:
```
System Boundary: Event Ticket Management System
  |
  ├── Identity Service Boundary
  │   └── Profile Management Domain
  │       ├── UC: Xem hồ sơ
  │       └── UC: Cập nhật hồ sơ
  │
  ├── Event Service Boundary
  │   ├── Event Management Domain
  │   │   ├── UC: Tạo sự kiện
  │   │   ├── UC: Cập nhật sự kiện
  │   │   ├── UC: Xóa sự kiện
  │   │   └── UC: Xem sự kiện của tôi
  │   └── Phase Management Domain
  │       ├── UC: Tạo phiên bán vé
  │       ├── UC: Cập nhật phiên bán vé
  │       └── UC: Xóa phiên bán vé
  │
  ├── Payment Service Boundary
  │   └── Transaction Management Domain
  │       └── UC: Xem giao dịch sự kiện
  │
  └── Ticket Service Boundary
      └── Ticket Management Domain
          └── UC: Xem vé đã bán
```

### Actors:
- **Primary Actor**: Organizer
- **Secondary Actors**: Storage Service, Email Service

### Include/Extend Relationships:
```
"Tạo sự kiện" <<include>> "Upload hình ảnh"
"Cập nhật sự kiện" <<extend>> "Upload hình ảnh mới"
"Tạo sự kiện" <<include>> "Xác thực quyền organizer"
```

## 3. USECASE DIAGRAM CHO USER

### Structure:
```
System Boundary: Event Ticket Management System
  |
  ├── Identity Service Boundary
  │   ├── Authentication Domain
  │   │   ├── UC: Đăng ký
  │   │   ├── UC: Đăng nhập
  │   │   ├── UC: Đăng xuất
  │   │   ├── UC: Quên mật khẩu
  │   │   ├── UC: Đặt lại mật khẩu
  │   │   └── UC: Đăng nhập Google
  │   └── Profile Management Domain
  │       ├── UC: Xem hồ sơ
  │       └── UC: Cập nhật hồ sơ
  │
  ├── Event Service Boundary (Public Access)
  │   └── Public Access Domain
  │       ├── UC: Xem danh sách sự kiện
  │       ├── UC: Tìm kiếm sự kiện
  │       ├── UC: Lọc sự kiện
  │       └── UC: Xem chi tiết sự kiện
  │
  ├── Ticket Service Boundary
  │   └── Ticket Management Domain
  │       ├── UC: Chọn vé
  │       ├── UC: Mua vé
  │       ├── UC: Xem vé của tôi
  │       └── UC: Xem mã QR vé
  │
  └── Payment Service Boundary
      └── Payment Management Domain
          ├── UC: Xử lý thanh toán
          └── UC: Xem lịch sử giao dịch
```

### Actors:
- **Primary Actor**: User
- **Secondary Actors**: VNPay, Email Service, Google OAuth, Storage Service

### Include/Extend Relationships:
```
"Đăng ký" <<include>> "Gửi email xác thực"
"Quên mật khẩu" <<include>> "Gửi email reset"
"Đăng nhập Google" <<include>> "Xác thực OAuth"
"Mua vé" <<include>> "Xử lý thanh toán"
"Xử lý thanh toán" <<include>> "Kết nối VNPay"
"Xem chi tiết sự kiện" <<extend>> "Đăng ký tham gia"
"Xem danh sách sự kiện" <<extend>> "Lọc theo danh mục"
"Xem danh sách sự kiện" <<extend>> "Tìm kiếm theo từ khóa"
```

## 4. USECASE DIAGRAM CHO GUEST

### Structure:
```
System Boundary: Event Ticket Management System
  |
  └── Event Service Boundary (Public Access Only)
      └── Public Access Domain
          ├── UC: Xem danh sách sự kiện công khai
          ├── UC: Tìm kiếm sự kiện
          ├── UC: Lọc sự kiện
          ├── UC: Xem chi tiết sự kiện
          ├── UC: Xem sự kiện nổi bật
          └── UC: Xem danh mục sự kiện
```

### Actors:
- **Primary Actor**: Guest
- **Secondary Actors**: Không có

### Include/Extend Relationships:
```
"Xem danh sách sự kiện" <<extend>> "Lọc theo danh mục"
"Xem danh sách sự kiện" <<extend>> "Tìm kiếm"
"Xem danh sách sự kiện" <<include>> "Xem sự kiện nổi bật"
```

## 5. NGUYÊN TẮC THIẾT KẾ

### 5.1. System Boundary
- Mỗi microservice có boundary riêng biệt
- Boundary được đặt tên rõ ràng theo service
- Sử dụng notation `<<subsystem>>`

### 5.2. Domain Grouping
- Các use case được nhóm theo domain logic
- Mỗi domain có boundary con trong service
- Tên domain phản ánh chức năng nghiệp vụ

### 5.3. Actor Classification
- **Primary Actors**: Người khởi xướng use case
- **Secondary Actors**: Hệ thống/dịch vụ hỗ trợ
- Sử dụng icon và notation phù hợp

### 5.4. Relationship Rules
- **Include**: Chức năng bắt buộc, không thể thiếu
- **Extend**: Chức năng tùy chọn, có thể có hoặc không
- **Generalization**: Kế thừa giữa các actor

### 5.5. Naming Convention
- Use case: Động từ + Tân ngữ (VD: "Tạo sự kiện")
- Actor: Danh từ chỉ vai trò (VD: "Admin", "User")
- System: Tên service + "Service" (VD: "Event Service")

## 6. TEMPLATE DRAWIO

### Cấu trúc file .drawio:
```xml
<mxfile>
  <diagram name="Admin UseCase">
    <!-- System boundary -->
    <object label="Event Ticket System" type="subsystem">
      <!-- Service boundaries -->
      <object label="Identity Service" type="subsystem">
        <!-- Domain groups -->
        <object label="User Management Domain" type="package">
          <!-- Use cases -->
          <object label="Quản lý người dùng" type="usecase"/>
        </object>
      </object>
    </object>
    
    <!-- Actors -->
    <object label="Admin" type="actor" stereotype="primary"/>
    <object label="Email Service" type="actor" stereotype="external"/>
    
    <!-- Relationships -->
    <connector type="association" source="Admin" target="Quản lý người dùng"/>
    <connector type="include" source="Use Case A" target="Use Case B"/>
  </diagram>
</mxfile>
```

## 7. CHECKLIST KIỂM TRA

### ✅ Structure Check
- [ ] Có System boundary rõ ràng
- [ ] Mỗi service có boundary riêng
- [ ] Domain được nhóm logic
- [ ] Use case có tên rõ ràng

### ✅ Actor Check
- [ ] Phân biệt Primary/Secondary actors
- [ ] External systems được thể hiện
- [ ] Actor có vai trò rõ ràng

### ✅ Relationship Check
- [ ] Include được dùng đúng (bắt buộc)
- [ ] Extend được dùng đúng (tùy chọn)
- [ ] Không có mối quan hệ thừa

### ✅ Compliance Check
- [ ] Tuân thủ UML 2.5 standard
- [ ] Phản ánh đúng kiến trúc microservices
- [ ] Dễ hiểu và maintain 