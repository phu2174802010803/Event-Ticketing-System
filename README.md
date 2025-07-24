# Event Ticketing System - Microservices Backend

## Tổng quan
Hệ thống bán vé sự kiện được xây dựng theo kiến trúc microservices với Spring Boot, sử dụng các công nghệ hiện đại.

## Kiến trúc hệ thống

### Services
- **Gateway App**: Gateway chính cho người dùng cuối
- **Gateway Admin-Organizer**: Gateway cho admin và organizer
- **Identity Service**: Quản lý xác thực và phân quyền
- **Event Service**: Quản lý sự kiện
- **Ticket Service**: Quản lý vé và bán vé
- **Payment Service**: Xử lý thanh toán
- **Notification Service**: Gửi thông báo

### Công nghệ sử dụng
- **Backend**: Spring Boot, Spring Cloud Gateway, Spring Security
- **Database**: MySQL, Redis
- **Message Queue**: Apache Kafka
- **Authentication**: JWT, Google OAuth2
- **Email Service**: SendGrid
- **Payment**: VNPay
- **Deployment**: Docker, Kubernetes
- **Storage**: DigitalOcean Spaces

## Cài đặt và Cấu hình

### Prerequisites
- Java 17 hoặc mới hơn
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Apache Kafka 2.8+

### Environment Variables
Tạo file `.env` hoặc set environment variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=event_ticketing
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key

# SendGrid Configuration
SENDGRID_API_KEY=your_sendgrid_api_key
SENDGRID_FROM_EMAIL=your_email@example.com
SENDGRID_FROM_NAME=Event Tickets

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# VNPay Configuration
VNPAY_MERCHANT_ID=your_vnpay_merchant_id
VNPAY_SECRET_KEY=your_vnpay_secret_key

# DigitalOcean Spaces Configuration
DO_SPACES_KEY=your_do_spaces_key
DO_SPACES_SECRET=your_do_spaces_secret
DO_SPACES_BUCKET=your_bucket_name
DO_SPACES_REGION=your_region
```

### Chạy ứng dụng

1. **Clone repository**
```bash
git clone https://github.com/phu2174802010803/Event-Ticketing-System.git
cd Event-Ticketing-System
```

2. **Build tất cả services**
```bash
mvn clean install
```

3. **Chạy từng service**
```bash
# Identity Service (Port 8081)
cd identity-service
mvn spring-boot:run

# Event Service (Port 8082)
cd event-service
mvn spring-boot:run

# Ticket Service (Port 8083)
cd ticket-service
mvn spring-boot:run

# Payment Service (Port 8084)
cd payment-service
mvn spring-boot:run

# Notification Service (Port 8085)
cd notification-service
mvn spring-boot:run

# Gateway App (Port 8080)
cd gateway-app
mvn spring-boot:run

# Gateway Admin-Organizer (Port 8086)
cd gateway-admin-organizer
mvn spring-boot:run
```

### Docker Deployment

```bash
# Build Docker images
docker-compose build

# Run services
docker-compose up -d
```

### Kubernetes Deployment

```bash
# Apply Kubernetes configurations
kubectl apply -f k8s/
```

## API Documentation

### Gateway Endpoints
- **User Gateway**: `http://localhost:8080`
- **Admin Gateway**: `http://localhost:8086`

### Service Endpoints
- **Identity Service**: `http://localhost:8081`
- **Event Service**: `http://localhost:8082`
- **Ticket Service**: `http://localhost:8083`
- **Payment Service**: `http://localhost:8084`
- **Notification Service**: `http://localhost:8085`

## Features

### Người dùng
- Đăng ký/Đăng nhập (Email, Google OAuth2)
- Tìm kiếm và xem thông tin sự kiện
- Mua vé online
- Quản lý vé đã mua
- Nhận thông báo qua email

### Organizer
- Tạo và quản lý sự kiện
- Thiết kế layout chỗ ngồi
- Quản lý vé bán ra
- Xem báo cáo doanh thu
- Quản lý giao dịch

### Admin
- Quản lý tất cả người dùng
- Quản lý categories
- Quản lý template layout
- Xem báo cáo tổng quan hệ thống
- Quản lý giao dịch toàn hệ thống

## Cấu hình bảo mật

### HTTPS/SSL
Để production, cần cấu hình SSL certificate:

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your_keystore_password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=your_key_alias
```

### CORS Configuration
Cấu hình CORS cho frontend:

```properties
cors.allowed.origins=http://localhost:3000,https://yourdomain.com
```

## Monitoring và Logging

### Application Logs
Logs được lưu trong thư mục `logs/` của mỗi service.

### Health Checks
- Health check endpoints: `/actuator/health`
- Metrics endpoints: `/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   - Kiểm tra connection string và credentials
   - Đảm bảo MySQL service đang chạy

2. **Redis Connection Issues**
   - Kiểm tra Redis service status
   - Verify Redis password

3. **Email Service Issues**
   - Kiểm tra SendGrid API key
   - Verify sender email domain

4. **Payment Issues**
   - Kiểm tra VNPay credentials
   - Verify webhook URLs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

Nếu gặp vấn đề, vui lòng tạo issue trên GitHub hoặc liên hệ support team.

## IMPORTANT: Configuration Setup

⚠️ **Trước khi chạy ứng dụng, bạn cần cấu hình các environment variables sau:**

1. **Tạo SendGrid account** và lấy API key
2. **Tạo Google OAuth2 app** và lấy client ID/secret
3. **Cấu hình VNPay merchant** (nếu sử dụng thanh toán)
4. **Setup MySQL database** với tên `event_ticketing`
5. **Cài đặt Redis server**

Không có các cấu hình này, ứng dụng sẽ không thể hoạt động đầy đủ.
