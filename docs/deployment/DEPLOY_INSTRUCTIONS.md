# 🚀 Hướng dẫn Deploy nhanh với file .env

## Bước 1: Push code lên GitHub (2 phút)

```bash
git add .
git commit -m "Ready for Render deployment"
git push origin main
```

## Bước 2: Tạo Web Service trên Render (3 phút)

1. Vào https://dashboard.render.com
2. Click **"New +"** → **"Web Service"**
3. Connect GitHub repository: **InternFlow**
4. Cấu hình:
   - **Name**: `internflow-backend` (hoặc tên bạn muốn)
   - **Runtime**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/InternFlow-0.0.1-SNAPSHOT.jar`
   - **Instance Type**: `Free`
5. **CHƯA** click "Create Web Service"

## Bước 3: Import Environment Variables (2 phút)

### Cách 1: Import từ file (Nhanh nhất)

1. Scroll xuống phần **Environment**
2. Click **"Add from .env"**
3. Mở file `.env.render` trong project
4. Copy **TOÀN BỘ** nội dung
5. Paste vào ô text
6. Click **"Add Variables"**

### Cách 2: Thêm thủ công (Nếu không có nút "Add from .env")

Mở file `.env.render` và thêm từng biến:

```
SPRING_PROFILES_ACTIVE=production
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
KEEP_ALIVE_ENDPOINT=/api/health/live
KEEP_ALIVE_ENABLED=true
DATABASE_URL=jdbc:postgresql://ep-nameless-unit-aowa4mmg-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=npg_6ZnjCYkRUFS0
CLOUDINARY_CLOUD_NAME=durw8vp93
CLOUDINARY_API_KEY=532238723367261
CLOUDINARY_API_SECRET=s5_kmlxKJks74N6X2J1OsjyGpO4
GOOGLE_OAUTH_CLIENT_ID=548332785385-5t4pcftrhba9f0fj7lsnqourqgt9ik6e.apps.googleusercontent.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tranthienloc.nina@gmail.com
MAIL_PASSWORD=vxwwiidpkioknqqg
INTERNFLOW_MAIL_TO=tuyendungbpns@gmail.com
INTERNFLOW_MAIL_CC=xuandat210425cty@gmail.com
```

## Bước 4: Deploy (1 phút)

1. Click **"Create Web Service"**
2. Render bắt đầu build và deploy
3. Xem **Logs** để theo dõi tiến trình

## Bước 5: Đợi Deploy xong (3-5 phút)

Trong Logs bạn sẽ thấy:
```
[INFO] Building jar: /app/target/InternFlow-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
Starting InternFlow...
Started InternFlowApplication in X.XXX seconds
```

Status chuyển sang: **Live** ✅

## Bước 6: Lấy URL và Test (1 phút)

URL của bạn: `https://internflow-backend.onrender.com` (hoặc tên bạn đặt)

### Test Health Check:
```bash
curl https://internflow-backend.onrender.com/api/health/live
```

Kết quả mong đợi:
```json
{"status":"UP"}
```

## Bước 7: Cập nhật Google OAuth (2 phút)

1. Vào https://console.cloud.google.com
2. Chọn project **internflow-496202**
3. **APIs & Services** → **Credentials**
4. Click vào OAuth 2.0 Client ID
5. Thêm **Authorized redirect URIs**:
   ```
   https://internflow-backend.onrender.com/login/oauth2/code/google
   https://internflow-backend.onrender.com
   ```
   (Thay `internflow-backend` bằng tên service của bạn)
6. Click **Save**

## ✅ Hoàn thành!

Backend đã live tại: `https://internflow-backend.onrender.com`

---

## 🔧 Nếu cần sửa Environment Variables

1. Vào Web Service → **Environment**
2. Sửa giá trị cần thay đổi
3. Click **"Save Changes"**
4. Service sẽ tự động redeploy

---

## 🐛 Troubleshooting

### Build failed
- Xem Logs để tìm lỗi
- Kiểm tra Java version = 17
- Verify Maven dependencies

### Database connection failed
- Kiểm tra `DATABASE_URL` đúng format
- Verify Neon database đang active
- Check username/password

### Email sending failed
- Verify `MAIL_PASSWORD` không có dấu cách
- Kiểm tra Gmail App Password còn hiệu lực
- Test gửi mail thủ công

### Application crashes
- Xem Application Logs
- Kiểm tra memory usage (Free tier: 512MB)
- Verify tất cả environment variables đã đúng

---

## 📝 Lưu ý

- Free tier service **sleep** sau 15 phút không dùng
- **Cold start** mất 30-60 giây
- Database Neon free tier: 0.5GB storage
- Nên test kỹ trên local trước khi deploy

---

## 🎯 Next Steps

- [ ] Test tất cả API endpoints
- [ ] Kết nối Frontend với Backend URL
- [ ] Setup monitoring
- [ ] Cân nhắc upgrade plan khi cần production

---

**Chúc bạn deploy thành công! 🎉**
