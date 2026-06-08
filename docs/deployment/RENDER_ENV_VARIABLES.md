# 🔑 Environment Variables cho Render.com

## Danh sách đầy đủ các biến cần điền

Vào **Web Service** → **Environment** → Click **"Add Environment Variable"**

### 1. Spring Profile
```
NAME: SPRING_PROFILES_ACTIVE
VALUE: production
```

```
NAME: SPRING_JPA_HIBERNATE_DDL_AUTO
VALUE: validate
```

```
NAME: KEEP_ALIVE_ENDPOINT
VALUE: /api/health/live
```

```
NAME: KEEP_ALIVE_ENABLED
VALUE: true
```

Production must use `validate`; do not use `update`, `create`, `create-drop`, or `drop` on Render.

---

### 2. Database (Neon.tech)

#### Cách 1: Dùng Connection String (Khuyến nghị)
```
NAME: DATABASE_URL
VALUE: postgresql://[user]:[password]@[host]/[database]?sslmode=require
```
**Ví dụ:**
```
postgresql://myuser:mypassword@ep-cool-darkness-123456.us-east-2.aws.neon.tech/internflow?sslmode=require
```

```
NAME: DATABASE_USERNAME
VALUE: [user từ Neon]
```

```
NAME: DATABASE_PASSWORD
VALUE: [password từ Neon]
```

#### Cách 2: Lấy từ Neon Dashboard
1. Vào https://console.neon.tech
2. Chọn project của bạn
3. Tab **Connection Details**
4. Copy từng thông tin:
   - Host: `ep-xxx-xxx.region.aws.neon.tech`
   - Database: `internflow`
   - User: `your-username`
   - Password: `your-password`

---

### 3. Cloudinary (Upload ảnh)

Lấy từ https://console.cloudinary.com/console

```
NAME: CLOUDINARY_CLOUD_NAME
VALUE: [cloud name của bạn]
```
**Ví dụ:** `durw8vp93`

```
NAME: CLOUDINARY_API_KEY
VALUE: [api key của bạn]
```
**Ví dụ:** `123456789012345`

```
NAME: CLOUDINARY_API_SECRET
VALUE: [api secret của bạn]
```
**Ví dụ:** `abcdefghijklmnopqrstuvwxyz123456`

**Cách lấy:**
1. Vào https://console.cloudinary.com
2. Dashboard → **Product Environment Credentials**
3. Copy: Cloud name, API Key, API Secret

---

### 4. Google OAuth

Lấy từ https://console.cloud.google.com

```
NAME: GOOGLE_OAUTH_CLIENT_ID
VALUE: [client id của bạn]
```
**Ví dụ:** `548332785385-5t4pcftrhba9f0fj7lsnqourqgt9ik6e.apps.googleusercontent.com`

```
NAME: GOOGLE_OAUTH_CLIENT_SECRET
VALUE: [client secret của bạn]
```
**Ví dụ:** `GOCSPX-xxxxxxxxxxxxxxxxxxxxx`

**Cách lấy:**
1. Vào https://console.cloud.google.com
2. Chọn project **internflow-496202**
3. **APIs & Services** → **Credentials**
4. Tìm **OAuth 2.0 Client IDs**
5. Click vào OAuth client name
6. Copy **Client ID** và **Client secret**

**⚠️ Lưu ý quan trọng:**
- Client Secret cần thiết để backend sử dụng Gmail API
- Không share Client Secret công khai
- Nếu mất, có thể reset trong Google Cloud Console

---

### 5. Email (Gmail SMTP)

```
NAME: MAIL_HOST
VALUE: smtp.gmail.com
```

```
NAME: MAIL_PORT
VALUE: 587
```

```
NAME: MAIL_USERNAME
VALUE: [email của bạn]
```
**Ví dụ:** `myemail@gmail.com`

```
NAME: MAIL_PASSWORD
VALUE: [Gmail App Password - 16 ký tự]
```
**Ví dụ:** `abcd efgh ijkl mnop` (không có dấu cách)

**⚠️ QUAN TRỌNG: Phải dùng App Password, KHÔNG phải mật khẩu Gmail thông thường!**

**Cách tạo Gmail App Password:**
1. Vào https://myaccount.google.com/security
2. Bật **2-Step Verification** (nếu chưa bật)
3. Tìm **App passwords** (hoặc search "app password")
4. Chọn app: **Mail**
5. Chọn device: **Other** → Nhập "Render InternFlow"
6. Click **Generate**
7. Copy password 16 ký tự (dạng: `xxxx xxxx xxxx xxxx`)
8. Dùng password này (bỏ dấu cách)

```
NAME: INTERNFLOW_MAIL_TO
VALUE: tuyendungbpns@gmail.com
```

```
NAME: INTERNFLOW_MAIL_CC
VALUE: xuandat210425cty@gmail.com
```

---

## 📋 Checklist

Đánh dấu khi đã điền xong:

- [ ] `SPRING_PROFILES_ACTIVE` = `production`
- [ ] `SPRING_JPA_HIBERNATE_DDL_AUTO` = `validate`
- [ ] `KEEP_ALIVE_ENDPOINT` = `/api/health/live`
- [ ] `KEEP_ALIVE_ENABLED` = `true`
- [ ] `DATABASE_URL` = Connection string từ Neon
- [ ] `DATABASE_USERNAME` = Username từ Neon
- [ ] `DATABASE_PASSWORD` = Password từ Neon
- [ ] `CLOUDINARY_CLOUD_NAME` = Cloud name
- [ ] `CLOUDINARY_API_KEY` = API Key
- [ ] `CLOUDINARY_API_SECRET` = API Secret
- [ ] `GOOGLE_OAUTH_CLIENT_ID` = Client ID
- [ ] `MAIL_HOST` = `smtp.gmail.com`
- [ ] `MAIL_PORT` = `587`
- [ ] `MAIL_USERNAME` = Gmail address
- [ ] `MAIL_PASSWORD` = Gmail App Password (16 ký tự)
- [ ] `INTERNFLOW_MAIL_TO` = Email nhận báo cáo
- [ ] `INTERNFLOW_MAIL_CC` = Email CC

**Tổng cộng: 14 biến**

---

## 🎯 Sau khi điền xong

1. Click **"Save Changes"**
2. Render sẽ tự động **redeploy** service
3. Đợi 3-5 phút để deploy xong
4. Kiểm tra **Logs** để đảm bảo không có lỗi
5. Test API: `https://your-app-name.onrender.com/api/health/live`

---

## ⚠️ Lưu ý bảo mật

- **KHÔNG** commit các giá trị này vào Git
- **KHÔNG** share credentials công khai
- Chỉ điền trực tiếp trên Render Dashboard
- Neon database nên enable SSL (`?sslmode=require`)

---

## 🐛 Troubleshooting

### Database connection failed
- Kiểm tra `DATABASE_URL` có đúng format không
- Đảm bảo có `?sslmode=require` ở cuối URL
- Verify username/password không có ký tự đặc biệt chưa encode

### Email sending failed
- Đảm bảo dùng **App Password**, không phải mật khẩu Gmail
- Kiểm tra 2-Step Verification đã bật
- Password không có dấu cách

### Cloudinary upload failed
- Verify API Key và Secret đúng
- Kiểm tra Cloud Name không có typo

### Google OAuth failed
- Client ID phải là OAuth Client ID, không phải Project ID
- Sau khi deploy xong, nhớ thêm Redirect URI vào Google Console

---

## 📝 Template Copy-Paste

```bash
SPRING_PROFILES_ACTIVE=production
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
KEEP_ALIVE_ENDPOINT=/api/health/live
KEEP_ALIVE_ENABLED=true
DATABASE_URL=postgresql://user:pass@host/db?sslmode=require
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
INTERNFLOW_MAIL_TO=tuyendungbpns@gmail.com
INTERNFLOW_MAIL_CC=xuandat210425cty@gmail.com
```

Thay thế các giá trị `your-xxx` bằng giá trị thực của bạn.
