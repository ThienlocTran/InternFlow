# 📦 InternFlow Backend - Render Deployment

Backend API cho hệ thống quản lý thực tập InternFlow, được xây dựng với Spring Boot và PostgreSQL.

## 🚀 Deploy lên Render.com

### Nhanh nhất (20 phút)
Đọc file **`QUICK_START.md`** - Hướng dẫn từng bước đơn giản

### Chi tiết đầy đủ
Đọc file **`RENDER_DEPLOYMENT.md`** - Hướng dẫn chi tiết với troubleshooting

### Checklist
Đọc file **`DEPLOYMENT_CHECKLIST.md`** - Danh sách kiểm tra từng bước

## 📁 Files đã chuẩn bị

```
InternFlow/
├── render.yaml                          # Cấu hình Blueprint cho Render
├── Dockerfile                           # Docker image (optional)
├── system.properties                    # Java version
├── .dockerignore                        # Loại trừ files khi build Docker
├── .env.example                         # Template environment variables
├── test-build.cmd / test-build.sh      # Script test build local
├── QUICK_START.md                       # Hướng dẫn nhanh
├── RENDER_DEPLOYMENT.md                 # Hướng dẫn chi tiết
├── DEPLOYMENT_CHECKLIST.md              # Checklist deploy
└── src/main/resources/
    ├── application.properties           # Config chung
    ├── application-local.properties     # Config local (gitignored)
    └── application-production.properties # Config production
```

## ⚙️ Tech Stack

- **Framework**: Spring Boot 4.0.6
- **Java**: 17
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Cloud Storage**: Cloudinary
- **Authentication**: Google OAuth 2.0
- **Email**: Gmail SMTP

## 🔑 Environment Variables cần thiết

```bash
# Database (Render tự động cung cấp)
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

# Cloudinary
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET

# Google OAuth
GOOGLE_OAUTH_CLIENT_ID

# Email
MAIL_USERNAME
MAIL_PASSWORD
INTERNFLOW_MAIL_TO
INTERNFLOW_MAIL_CC
```

Chi tiết xem file `.env.example`

## 🧪 Test Build Local

Trước khi deploy, test build local:

```bash
# Windows
test-build.cmd

# Linux/Mac
chmod +x test-build.sh
./test-build.sh
```

## 📊 Render Free Tier

- **Web Service**: 512MB RAM, 750 giờ/tháng
- **Database**: 1GB storage, 97 giờ connection/tháng
- **Sleep**: Sau 15 phút không hoạt động
- **Cold Start**: 30-60 giây

## 🔗 Useful Links

- [Render Dashboard](https://dashboard.render.com)
- [Render Docs](https://render.com/docs)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)

## 📝 Deployment Steps Summary

1. ✅ Test build local
2. ✅ Push code lên GitHub
3. ✅ Tạo PostgreSQL database trên Render
4. ✅ Deploy web service (Blueprint hoặc Manual)
5. ✅ Thêm environment variables
6. ✅ Đợi deploy xong
7. ✅ Test API
8. ✅ Cập nhật Google OAuth redirect URIs

## 🐛 Troubleshooting

### Build Failed
- Kiểm tra Java version = 17
- Xem Maven logs để tìm lỗi dependency

### Database Connection Failed
- Verify DATABASE_URL, USERNAME, PASSWORD
- Kiểm tra database đang running

### Application Crashes
- Xem Application Logs
- Kiểm tra memory usage (Free tier: 512MB)

### Slow Response
- Free tier service sleep sau 15 phút
- Cold start mất 30-60 giây
- Cân nhắc upgrade plan

## 💰 Upgrade to Production

Khi cần performance tốt hơn:

- **Web Service Starter**: $7/month
  - 512MB RAM
  - Không sleep
  - Faster response

- **Database Starter**: $7/month
  - 1GB RAM
  - 10GB storage
  - Unlimited connections

## 📞 Support

Nếu gặp vấn đề:
1. Đọc `DEPLOYMENT_CHECKLIST.md`
2. Xem Logs trong Render Dashboard
3. Tham khảo [Render Community](https://community.render.com)

---

**Chúc bạn deploy thành công! 🎉**

## Health endpoints and Render keep-alive

Use `GET /api/health/live` for Render health checks and keep-alive pings. This endpoint returns a lightweight liveness response with `dbChecked=false` and does not call repositories, services, `DataSource`, or PostgreSQL.

`GET /api/ping` is also lightweight and remains available for compatibility.

Use `GET /api/health/ready` only for deploy/debug readiness checks. It opens a database connection and validates PostgreSQL, so it can wake Neon compute and must not be used for automatic keep-alive traffic.

The keep-alive scheduler defaults to `KEEP_ALIVE_ENDPOINT=/api/health/live`. Do not configure it to `/api/health/ready` or `/actuator/health`; if Spring Boot Actuator is added later, its default health endpoint may include `DataSourceHealthIndicator` and check the database.

