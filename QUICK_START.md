# 🚀 Quick Start - Deploy lên Render.com

## Bước 1: Test Build Local (5 phút)

```bash
# Windows
test-build.cmd

# Linux/Mac
chmod +x test-build.sh
./test-build.sh
```

Nếu build thành công → Tiếp tục bước 2

## Bước 2: Push lên GitHub (2 phút)

```bash
git add .
git commit -m "Ready for Render deployment"
git push origin main
```

## Bước 3: Lấy thông tin Database từ Neon.tech (2 phút)

1. Vào https://console.neon.tech
2. Chọn project **InternFlow** của bạn
3. Vào tab **Dashboard** hoặc **Connection Details**
4. **LƯU LẠI** các thông tin sau:
   - **Connection String**: `postgresql://user:password@host/database`
   - Hoặc riêng lẻ:
     - Host
     - Database name
     - User
     - Password
5. Đảm bảo database đang **Active**

## Bước 4: Deploy Web Service (5 phút)

### Cách 1: Dùng Blueprint (Tự động - Khuyến nghị)

1. Click **"New +"** → **"Blueprint"**
2. Connect GitHub repository của bạn
3. Render tự động phát hiện `render.yaml`
4. Click **"Apply"**
5. Chờ deploy xong → Chuyển sang Bước 5

### Cách 2: Thủ công

1. Click **"New +"** → **"Web Service"**
2. Connect GitHub repository
3. Cấu hình:
   - Name: `internflow-backend`
   - Runtime: **Java**
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/InternFlow-0.0.1-SNAPSHOT.jar`
   - Instance Type: **Free**
4. Click **"Create Web Service"**

## Bước 5: Thêm Environment Variables (5 phút)

Vào **Web Service** → **Environment** → **Add Environment Variable**

Copy từng dòng này và điền giá trị:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=production

# Database (lấy từ Neon.tech ở Bước 3)
DATABASE_URL=<neon-connection-string>
DATABASE_USERNAME=<neon-username>
DATABASE_PASSWORD=<neon-password>

# Cloudinary (lấy từ cloudinary.com)
CLOUDINARY_CLOUD_NAME=<your-cloud-name>
CLOUDINARY_API_KEY=<your-api-key>
CLOUDINARY_API_SECRET=<your-api-secret>

# Google OAuth (lấy từ Google Cloud Console)
GOOGLE_OAUTH_CLIENT_ID=<your-client-id>

# Gmail (dùng App Password, không phải mật khẩu thường)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your-email@gmail.com>
MAIL_PASSWORD=<gmail-app-password>
INTERNFLOW_MAIL_TO=tuyendungbpns@gmail.com
INTERNFLOW_MAIL_CC=xuandat210425cty@gmail.com
```

Click **"Save Changes"** → Service sẽ tự động redeploy

## Bước 6: Đợi Deploy Xong (3-5 phút)

Xem **Logs** để theo dõi:
- Build Maven
- Download dependencies
- Start Spring Boot
- Status: **Live** ✅

## Bước 7: Lấy URL và Test (2 phút)

URL của bạn: `https://your-app-name.onrender.com`

Test API:
```bash
curl https://your-app-name.onrender.com/actuator/health
```

Kết quả mong đợi:
```json
{"status":"UP"}
```

## Bước 8: Cập nhật Google OAuth (2 phút)

1. Vào [Google Cloud Console](https://console.cloud.google.com)
2. Chọn project của bạn
3. **APIs & Services** → **Credentials**
4. Click vào OAuth 2.0 Client ID của bạn
5. Thêm vào **Authorized redirect URIs**:
   ```
   https://your-app-name.onrender.com/login/oauth2/code/google
   https://your-app-name.onrender.com
   ```
6. Click **Save**

## ✅ Hoàn thành!

Backend của bạn đã live tại: `https://your-app-name.onrender.com`

## 📝 Lưu ý quan trọng

### Free Tier Limitations
- Service **sleep** sau 15 phút không dùng
- **Cold start** mất 30-60 giây khi wake up
- Database: 1GB storage, 97 giờ connection/tháng

### Tạo Gmail App Password
1. Vào https://myaccount.google.com/security
2. Bật **2-Step Verification**
3. Tìm **App passwords**
4. Tạo password cho **Mail**
5. Dùng password 16 ký tự này cho `MAIL_PASSWORD`

### Nếu có lỗi
1. Xem **Logs** trong Render Dashboard
2. Kiểm tra **Environment Variables** đã đúng chưa
3. Verify Database đang running
4. Đọc file `DEPLOYMENT_CHECKLIST.md` để troubleshoot

## 🎯 Next Steps

- [ ] Test tất cả API endpoints
- [ ] Kết nối Frontend với Backend URL mới
- [ ] Chạy database migrations (nếu cần)
- [ ] Setup monitoring
- [ ] Cân nhắc upgrade plan khi cần

## 📚 Tài liệu chi tiết

- `RENDER_DEPLOYMENT.md` - Hướng dẫn đầy đủ
- `DEPLOYMENT_CHECKLIST.md` - Checklist từng bước
- `.env.example` - Template environment variables

## 🆘 Cần trợ giúp?

- Render Docs: https://render.com/docs
- Render Community: https://community.render.com
