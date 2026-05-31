# ✅ Checklist Deploy InternFlow lên Render.com

## Trước khi Deploy

### 1. Chuẩn bị Code
- [ ] Code đã được test kỹ trên local
- [ ] Tất cả dependencies trong `pom.xml` đều cần thiết
- [ ] Không có hardcoded credentials trong code
- [ ] `.gitignore` đã loại trừ các file nhạy cảm

### 2. Chuẩn bị Database
- [ ] Có file migration SQL (nếu cần)
- [ ] Biết cấu trúc database cần thiết
- [ ] Đã backup data quan trọng (nếu có)

### 3. Chuẩn bị Credentials

#### Cloudinary
- [ ] Có Cloudinary account
- [ ] Lấy được: cloud_name, api_key, api_secret
- [ ] Test upload ảnh thành công

#### Google OAuth
- [ ] Có Google Cloud project
- [ ] Đã tạo OAuth 2.0 Client ID
- [ ] Lấy được Client ID
- [ ] Sẽ thêm Redirect URI sau khi có domain Render

#### Gmail
- [ ] Có Gmail account để gửi mail
- [ ] Đã bật 2-Step Verification
- [ ] Đã tạo App Password (16 ký tự)
- [ ] Test gửi mail thành công

### 4. Push lên GitHub
- [ ] Repository đã public hoặc Render có quyền truy cập
- [ ] Branch main/master có code mới nhất
- [ ] Đã commit tất cả file cấu hình mới

```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin main
```

## Trong quá trình Deploy

### 1. Tạo Database trên Render
- [ ] Vào Render Dashboard
- [ ] New + → PostgreSQL
- [ ] Name: `internflow-db`
- [ ] Region: Singapore (gần VN nhất)
- [ ] Plan: Free (hoặc Starter nếu cần)
- [ ] Click "Create Database"
- [ ] **Lưu lại**: Internal Database URL, Username, Password

### 2. Deploy Web Service

#### Nếu dùng Blueprint (render.yaml)
- [ ] New + → Blueprint
- [ ] Connect GitHub repository
- [ ] Render phát hiện `render.yaml`
- [ ] Click "Apply"

#### Nếu deploy thủ công
- [ ] New + → Web Service
- [ ] Connect GitHub repository
- [ ] Runtime: Java
- [ ] Build Command: `./mvnw clean package -DskipTests`
- [ ] Start Command: `java -jar target/InternFlow-0.0.1-SNAPSHOT.jar`
- [ ] Instance Type: Free

### 3. Cấu hình Environment Variables

Vào Web Service → Environment → Add Environment Variables:

- [ ] `SPRING_PROFILES_ACTIVE` = `production`
- [ ] `DATABASE_URL` = (từ Render Database)
- [ ] `DATABASE_USERNAME` = (từ Render Database)
- [ ] `DATABASE_PASSWORD` = (từ Render Database)
- [ ] `CLOUDINARY_CLOUD_NAME` = (từ Cloudinary)
- [ ] `CLOUDINARY_API_KEY` = (từ Cloudinary)
- [ ] `CLOUDINARY_API_SECRET` = (từ Cloudinary)
- [ ] `GOOGLE_OAUTH_CLIENT_ID` = (từ Google Cloud)
- [ ] `MAIL_HOST` = `smtp.gmail.com`
- [ ] `MAIL_PORT` = `587`
- [ ] `MAIL_USERNAME` = (Gmail address)
- [ ] `MAIL_PASSWORD` = (Gmail App Password)
- [ ] `INTERNFLOW_MAIL_TO` = `tuyendungbpns@gmail.com`
- [ ] `INTERNFLOW_MAIL_CC` = `xuandat210425cty@gmail.com`

### 4. Chờ Build & Deploy
- [ ] Xem Logs để theo dõi quá trình build
- [ ] Build thành công (không có lỗi)
- [ ] Application start thành công
- [ ] Service status: "Live"


### 4. Keep-alive endpoint
- [ ] `KEEP_ALIVE_ENDPOINT` = `/api/health/live`
- [ ] Khong dung `/api/health/ready` hoac `/actuator/health` cho keep-alive vi cac endpoint nay co the cham database va lam Neon compute active.

## Sau khi Deploy

### 1. Cập nhật Google OAuth
- [ ] Lấy URL của app: `https://your-app-name.onrender.com`
- [ ] Vào Google Cloud Console
- [ ] OAuth 2.0 Client → Edit
- [ ] Thêm Authorized redirect URIs:
  - `https://your-app-name.onrender.com/login/oauth2/code/google`
  - `https://your-app-name.onrender.com`
- [ ] Save

### 2. Chạy Database Migration (nếu cần)
- [ ] Connect vào PostgreSQL trên Render
- [ ] Chạy file migration SQL
- [ ] Verify tables đã được tạo

```bash
# Lấy connection string từ Render Dashboard
psql <connection-string>

# Trong psql
\i db/migrations/2026-05-31-r2-foundation-shift-photo-source.sql
\dt  # List tables
\q   # Quit
```

### 3. Test API

#### Health Check
```bash
curl https://your-app-name.onrender.com/api/health/live
```
- [ ] Response includes `"status":"UP"` and `"dbChecked":false`

#### Test Authentication
```bash
curl https://your-app-name.onrender.com/api/auth/status
```
- [ ] Response không phải 500 error

#### Test từ Frontend
- [ ] Cập nhật API URL trong frontend
- [ ] Test login
- [ ] Test các chức năng chính

### 4. Monitoring
- [ ] Xem Metrics trong Render Dashboard
- [ ] Kiểm tra Memory usage
- [ ] Kiểm tra Response time
- [ ] Setup alerts (nếu cần)

### 5. Cập nhật CORS (nếu cần)
- [ ] Kiểm tra `CorsConfig.java`
- [ ] Thêm domain frontend vào allowed origins
- [ ] Redeploy nếu có thay đổi

## Troubleshooting

### Build Failed
- [ ] Kiểm tra Java version trong logs
- [ ] Kiểm tra Maven dependencies
- [ ] Xem chi tiết lỗi trong Build Logs

### Application Crashes
- [ ] Xem Application Logs
- [ ] Kiểm tra Database connection
- [ ] Kiểm tra Environment Variables
- [ ] Kiểm tra Memory limits (Free: 512MB)

### Database Connection Failed
- [ ] Verify DATABASE_URL format đúng
- [ ] Verify USERNAME và PASSWORD
- [ ] Kiểm tra Database đang running
- [ ] Kiểm tra connection pool settings

### Slow Performance
- [ ] Free tier có giới hạn resources
- [ ] Service sleep sau 15 phút không dùng
- [ ] Cold start mất 30-60 giây
- [ ] Cân nhắc upgrade plan

## Production Ready

### Khi sẵn sàng cho Production
- [ ] Upgrade Web Service plan ($7/month)
- [ ] Upgrade Database plan ($7/month)
- [ ] Setup Custom Domain
- [ ] Enable Auto-Deploy
- [ ] Setup Monitoring & Alerts
- [ ] Backup strategy
- [ ] SSL Certificate (tự động với Render)

## Notes

- Free tier service sẽ sleep sau 15 phút không hoạt động
- Cold start mất khoảng 30-60 giây
- Database free tier: 1GB storage, 97 giờ connection/tháng
- Nên test kỹ trên local trước khi deploy
- Giữ credentials an toàn, không commit vào Git

## Liên hệ

- Render Docs: https://render.com/docs
- Render Community: https://community.render.com
- Spring Boot Docs: https://spring.io/projects/spring-boot
