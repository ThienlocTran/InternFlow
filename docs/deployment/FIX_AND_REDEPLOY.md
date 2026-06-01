# 🔧 Fix Health Check Endpoint và Redeploy

## ❌ Vấn đề hiện tại

Backend đã deploy thành công nhưng endpoint `/api/health` bị lỗi:
```
NoResourceFoundException: No static resource api/health
```

## ✅ Đã fix

Tôi đã tạo `HealthController.java` với lightweight endpoints:
- `GET /api/health/live` - liveness cho Render/UptimeRobot/keep-alive, không chạm DB, `dbChecked=false`
- `GET /api/health` - compatibility lightweight, không chạm DB, `dbChecked=false`
- `GET /api/ping` - Simple ping/pong, không chạm DB

`GET /api/health/ready` chỉ dùng để debug/deploy readiness thủ công. Endpoint này có DB check và có thể wake Neon, không dùng cho keep-alive/smoke check định kỳ.

## 🚀 Cách Redeploy

### Bước 1: Commit và Push code mới

```bash
cd d:\CheckSV\InternFlow

git add .
git commit -m "Add health check endpoint"
git push origin main
```

### Bước 2: Redeploy trên Render

#### Cách 1: Auto Deploy (Nếu đã bật)
- Render sẽ tự động detect push mới
- Tự động build và deploy
- Đợi 3-5 phút

#### Cách 2: Manual Deploy
1. Vào Render Dashboard
2. Chọn Web Service **InternFlow**
3. Click **"Manual Deploy"** → **"Deploy latest commit"**
4. Đợi build xong

### Bước 3: Test sau khi deploy

#### Test Health Check:
```bash
curl https://internflow-e1to.onrender.com/api/health/live
```

**Kết quả mong đợi:**
```json
{
  "dbChecked": false,
  "status": "UP",
  "service": "InternFlow",
  "timestamp": "..."
}
```

UptimeRobot URL nên cấu hình:
`https://internflow-e1to.onrender.com/api/health/live`

#### Test Ping:
```bash
curl https://internflow-e1to.onrender.com/api/ping
```

**Kết quả mong đợi:**
```
pong
```

---

## 📝 Lưu ý về các lỗi trong log

### 1. ✅ Lỗi `/` và `/favicon.ico` - KHÔNG CẦN FIX
```
NoResourceFoundException: No static resource  for request '/'
NoResourceFoundException: No static resource favicon.ico
```

**Giải thích**: 
- Đây là backend API, không serve frontend
- Browser tự động request `/` và `/favicon.ico`
- Hoàn toàn bình thường, không ảnh hưởng

**Nếu muốn tắt warning** (optional):
Thêm vào `application-production.properties`:
```properties
spring.mvc.log-resolved-exception=false
```

### 2. ✅ Service đã chạy thành công
```
Available at your primary URL https://internflow-e1to.onrender.com
Detected service running on port 10000
```

Backend đã UP và running!

---

## 🎯 Sau khi redeploy xong

### 1. Verify Health Check
```bash
curl https://internflow-e1to.onrender.com/api/health/live
```

### 2. Test API endpoints khác
```bash
# Test auth endpoint (nếu có)
curl https://internflow-e1to.onrender.com/api/auth/status

# Test với frontend
# Cập nhật VITE_API_BASE_URL trong Vercel
```

### 3. Cập nhật CORS
Sau khi deploy frontend lên Vercel, nhớ cập nhật:
```
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

---

## 🐛 Nếu vẫn có lỗi

### Check Logs
1. Vào Render Dashboard
2. Tab **Logs**
3. Tìm dòng `Started InternFlowApplication`
4. Kiểm tra có lỗi gì không

### Common Issues

**Build failed:**
- Kiểm tra code compile được local không
- Xem Maven logs

**Application crashes:**
- Kiểm tra Environment Variables
- Verify Database connection
- Check memory usage

**404 on /api/health:**
- Verify `HealthController.java` đã được commit
- Check SecurityConfig cho phép `/api/**`
- Xem logs có load controller không

---

## ✅ Checklist

- [ ] Đã tạo `HealthController.java`
- [ ] Commit và push code
- [ ] Redeploy trên Render
- [ ] Đợi deploy xong (status: Live)
- [ ] Test `/api/health/live` thành công
- [ ] Confirm UptimeRobot/Render/keep-alive use `/api/health/live`, not `/api/health/ready` or `/actuator/health`
- [ ] Test `/api/ping` thành công
- [ ] Sẵn sàng deploy frontend

---

**Backend URL của bạn:** `https://internflow-e1to.onrender.com`

Sau khi fix xong, bạn có thể tiếp tục deploy frontend lên Vercel! 🎉
