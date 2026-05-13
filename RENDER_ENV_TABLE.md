# 📋 Environment Variables - Sẵn sàng Copy/Paste

## Bảng đầy đủ các biến (14 biến)

| # | NAME | VALUE |
|---|------|-------|
| 1 | `SPRING_PROFILES_ACTIVE` | `production` |
| 2 | `DATABASE_URL` | `jdbc:postgresql://ep-nameless-unit-aowa4mmg-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require` |
| 3 | `DATABASE_USERNAME` | `neondb_owner` |
| 4 | `DATABASE_PASSWORD` | `npg_6ZnjCYkRUFS0` |
| 5 | `CLOUDINARY_CLOUD_NAME` | `durw8vp93` |
| 6 | `CLOUDINARY_API_KEY` | `532238723367261` |
| 7 | `CLOUDINARY_API_SECRET` | `s5_kmlxKJks74N6X2J1OsjyGpO4` |
| 8 | `GOOGLE_OAUTH_CLIENT_ID` | `548332785385-5t4pcftrhba9f0fj7lsnqourqgt9ik6e.apps.googleusercontent.com` |
| 9 | `MAIL_HOST` | `smtp.gmail.com` |
| 10 | `MAIL_PORT` | `587` |
| 11 | `MAIL_USERNAME` | **[ĐIỀN EMAIL GMAIL CỦA BẠN]** |
| 12 | `MAIL_PASSWORD` | **[ĐIỀN GMAIL APP PASSWORD]** |
| 13 | `INTERNFLOW_MAIL_TO` | `tuyendungbpns@gmail.com` |
| 14 | `INTERNFLOW_MAIL_CC` | `xuandat210425cty@gmail.com` |

---

## 🚀 Cách sử dụng

### Trong Render Dashboard:

1. Vào **Web Service** → **Environment**
2. Click **"Add Environment Variable"**
3. Copy NAME từ cột 2
4. Copy VALUE từ cột 3
5. Click **"Add"**
6. Lặp lại cho 14 biến

---

## ⚠️ Cần điền thêm (2 biến)

### MAIL_USERNAME
- Điền email Gmail của bạn
- Ví dụ: `myemail@gmail.com`

### MAIL_PASSWORD
- **KHÔNG** phải mật khẩu Gmail thông thường
- Phải là **Gmail App Password** (16 ký tự)

**Cách tạo Gmail App Password:**

1. Vào: https://myaccount.google.com/security
2. Bật **2-Step Verification** (nếu chưa)
3. Tìm **"App passwords"** (hoặc search)
4. Chọn:
   - App: **Mail**
   - Device: **Other** → Nhập "Render InternFlow"
5. Click **"Generate"**
6. Copy password 16 ký tự (dạng: `xxxx xxxx xxxx xxxx`)
7. Paste vào Render (bỏ dấu cách)

---

## ✅ Checklist

- [ ] Đã điền 12 biến có sẵn giá trị
- [ ] Đã điền `MAIL_USERNAME` (email Gmail)
- [ ] Đã tạo và điền `MAIL_PASSWORD` (App Password)
- [ ] Click "Save Changes"
- [ ] Đợi Render redeploy (3-5 phút)
- [ ] Kiểm tra Logs không có lỗi
- [ ] Test API health check

---

## 🎯 Sau khi deploy xong

### 1. Test Health Check
```bash
curl https://your-app-name.onrender.com/actuator/health
```

Kết quả mong đợi:
```json
{"status":"UP"}
```

### 2. Cập nhật Google OAuth Redirect URI

Vào Google Cloud Console:
1. https://console.cloud.google.com
2. Chọn project **internflow-496202**
3. **APIs & Services** → **Credentials**
4. Click vào OAuth Client ID
5. Thêm **Authorized redirect URIs**:
   ```
   https://your-app-name.onrender.com/login/oauth2/code/google
   https://your-app-name.onrender.com
   ```
6. Click **Save**

---

## 📝 Format cho Render (Copy toàn bộ)

Nếu Render hỗ trợ import bulk, copy đoạn này:

```
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=jdbc:postgresql://ep-nameless-unit-aowa4mmg-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=npg_6ZnjCYkRUFS0
CLOUDINARY_CLOUD_NAME=durw8vp93
CLOUDINARY_API_KEY=532238723367261
CLOUDINARY_API_SECRET=s5_kmlxKJks74N6X2J1OsjyGpO4
GOOGLE_OAUTH_CLIENT_ID=548332785385-5t4pcftrhba9f0fj7lsnqourqgt9ik6e.apps.googleusercontent.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=[YOUR_GMAIL]
MAIL_PASSWORD=[YOUR_APP_PASSWORD]
INTERNFLOW_MAIL_TO=tuyendungbpns@gmail.com
INTERNFLOW_MAIL_CC=xuandat210425cty@gmail.com
```

**Nhớ thay thế:**
- `[YOUR_GMAIL]` → Email Gmail của bạn
- `[YOUR_APP_PASSWORD]` → Gmail App Password 16 ký tự
