# Hướng dẫn Deploy InternFlow Backend lên Render.com

## Chuẩn bị

### 1. Tạo tài khoản Render.com
- Truy cập [render.com](https://render.com)
- Đăng ký/Đăng nhập bằng GitHub account

### 2. Push code lên GitHub
```bash
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

## Các bước Deploy

### Phương án 1: Deploy tự động với render.yaml (Khuyến nghị)

1. **Tạo PostgreSQL Database**
   - Vào Dashboard Render → Click "New +"
   - Chọn "PostgreSQL"
   - Điền thông tin:
     - Name: `internflow-db`
     - Database: `internflow`
     - User: `internflow`
     - Region: Chọn gần nhất (Singapore cho VN)
     - Plan: Free
   - Click "Create Database"
   - **Lưu lại** connection string, username, password

2. **Deploy Web Service**
   - Vào Dashboard → Click "New +"
   - Chọn "Blueprint"
   - Connect GitHub repository của bạn
   - Render sẽ tự động phát hiện file `render.yaml`
   - Click "Apply"

3. **Cấu hình Environment Variables**
   
   Render sẽ tự động tạo service, nhưng bạn cần thêm các biến môi trường sau:
   
   Vào Web Service → Environment → Add Environment Variables:
   
   ```
   CLOUDINARY_CLOUD_NAME=<your-cloudinary-cloud-name>
   CLOUDINARY_API_KEY=<your-cloudinary-api-key>
   CLOUDINARY_API_SECRET=<your-cloudinary-api-secret>
   GOOGLE_OAUTH_CLIENT_ID=<your-google-oauth-client-id>
   MAIL_USERNAME=<your-gmail-address>
   MAIL_PASSWORD=<your-gmail-app-password>
   INTERNFLOW_MAIL_TO=tuyendungbpns@gmail.com
   INTERNFLOW_MAIL_CC=xuandat210425cty@gmail.com
   ```

### Phương án 2: Deploy thủ công

1. **Tạo PostgreSQL Database** (giống phương án 1)

2. **Tạo Web Service**
   - Dashboard → "New +" → "Web Service"
   - Connect GitHub repository
   - Cấu hình:
     - Name: `internflow-backend`
     - Runtime: `Java`
     - Build Command: `./mvnw clean package -DskipTests`
     - Start Command: `java -jar target/InternFlow-0.0.1-SNAPSHOT.jar`
     - Instance Type: Free

3. **Thêm Environment Variables** (giống phương án 1)

## Lưu ý quan trọng

### 1. Gmail App Password
- Không dùng mật khẩu Gmail thông thường
- Tạo App Password:
  1. Vào Google Account → Security
  2. Bật 2-Step Verification
  3. Tạo App Password cho "Mail"
  4. Dùng password này cho `MAIL_PASSWORD`

### 2. Google OAuth Client ID
- Cần thêm Authorized redirect URIs trong Google Cloud Console:
  ```
  https://your-app-name.onrender.com/login/oauth2/code/google
  https://your-app-name.onrender.com
  ```

### 3. Database Migration
- File migration SQL trong `db/migrations/` cần chạy thủ công
- Kết nối vào PostgreSQL trên Render:
  ```bash
  psql <connection-string-from-render>
  ```
- Chạy migration:
  ```sql
\i db/migrations/2026-05-31-r2-foundation-shift-photo-source.sql
  ```

### 4. CORS Configuration
- Kiểm tra `CorsConfig.java` đã cho phép domain của frontend
- Thêm domain Render vào allowed origins nếu cần

### 5. Free Tier Limitations
- Service sẽ sleep sau 15 phút không hoạt động
- Cold start mất ~30-60 giây
- 750 giờ/tháng miễn phí
- Database: 1GB storage, 97 giờ connection time/tháng

## Kiểm tra sau khi deploy

1. **Health Check**
   ```bash
   curl https://your-app-name.onrender.com/api/health/live
   ```

2. **Xem Logs**
   - Vào Web Service → Logs
   - Kiểm tra lỗi startup

3. **Test API**
   ```bash
   curl https://your-app-name.onrender.com/api/health
   ```

## Troubleshooting

### Build failed
- Kiểm tra Java version (phải là 17)
- Xem logs để tìm lỗi dependency

### Database connection failed
- Kiểm tra DATABASE_URL, USERNAME, PASSWORD
- Verify database đang running

### Application crashes
- Kiểm tra memory limits (free tier: 512MB)
- Xem logs để tìm lỗi

### Slow performance
- Free tier có giới hạn CPU/Memory
- Cân nhắc upgrade plan nếu cần

## Nâng cấp Production

Khi sẵn sàng production:

1. **Upgrade Plan**
   - Web Service: Starter ($7/month) - 512MB RAM, không sleep
   - Database: Starter ($7/month) - 1GB RAM, 10GB storage

2. **Thêm Custom Domain**
   - Settings → Custom Domain
   - Cấu hình DNS records

3. **Enable Auto-Deploy**
   - Settings → Auto-Deploy: Yes
   - Mỗi lần push code sẽ tự động deploy

4. **Monitoring**
   - Xem metrics trong Dashboard
   - Setup alerts cho downtime

## Liên hệ hỗ trợ

- Render Docs: https://render.com/docs
- Community: https://community.render.com

## Health endpoints and Render keep-alive

Use `GET /api/health/live` for Render health checks and keep-alive pings. This endpoint returns a lightweight liveness response with `dbChecked=false` and does not call repositories, services, `DataSource`, or PostgreSQL.

`GET /api/ping` is also lightweight and remains available for compatibility.

Use `GET /api/health/ready` only for deploy/debug readiness checks. It opens a database connection and validates PostgreSQL, so it can wake Neon compute and must not be used for automatic keep-alive traffic.

The keep-alive scheduler defaults to `KEEP_ALIVE_ENDPOINT=/api/health/live`. Do not configure it to `/api/health/ready` or `/actuator/health`; if Spring Boot Actuator is added later, its default health endpoint may include `DataSourceHealthIndicator` and check the database.

