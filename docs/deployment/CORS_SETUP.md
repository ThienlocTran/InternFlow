# 🔗 Cấu hình CORS cho Frontend Vercel

## ✅ Đã cập nhật

Tôi đã cập nhật `CorsConfig.java` để hỗ trợ:
- ✓ Localhost (development)
- ✓ Frontend Vercel (production)
- ✓ Dùng environment variable linh hoạt

## 🎯 Cách hoạt động

CORS sẽ tự động cho phép:
1. **Localhost**: `http://localhost:5173`, `http://localhost:5174` (luôn luôn)
2. **Custom domains**: Từ biến `CORS_ALLOWED_ORIGINS`

## 📝 Cấu hình trong Render

### Bước 1: Lấy URL Frontend Vercel

Sau khi deploy frontend lên Vercel, bạn sẽ có URL dạng:
- `https://internflow.vercel.app`
- hoặc `https://your-project-name.vercel.app`

### Bước 2: Thêm vào Environment Variables

Trong file `.env.render`, dòng cuối cùng:

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app
```

**Nếu có nhiều domain**, phân cách bằng dấu phẩy:

```bash
CORS_ALLOWED_ORIGINS=https://internflow.vercel.app,https://internflow-staging.vercel.app,https://custom-domain.com
```

### Bước 3: Cập nhật trên Render

#### Nếu chưa deploy:
- Sửa file `.env.render` với URL Vercel thực tế
- Push code lên GitHub
- Deploy như bình thường

#### Nếu đã deploy rồi:
1. Vào Render Dashboard
2. Chọn Web Service **InternFlow**
3. Tab **Environment**
4. Tìm biến `CORS_ALLOWED_ORIGINS`
5. Sửa value thành URL Vercel của bạn
6. Click **"Save Changes"**
7. Service sẽ tự động redeploy

## 🧪 Test CORS

### Từ Browser Console (Frontend)

```javascript
fetch('https://internflow-backend.onrender.com/api/health/live')
  .then(res => res.json())
  .then(data => console.log('CORS OK:', data))
  .catch(err => console.error('CORS Error:', err));
```

### Kiểm tra Response Headers

Mở DevTools → Network → Chọn request → Headers

Phải có:
```
Access-Control-Allow-Origin: https://your-frontend.vercel.app
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
```

## ⚠️ Lưu ý quan trọng

### 1. HTTPS vs HTTP
- Vercel luôn dùng **HTTPS**
- Đảm bảo URL trong `CORS_ALLOWED_ORIGINS` có `https://`
- **KHÔNG** có dấu `/` ở cuối URL

✅ Đúng: `https://internflow.vercel.app`
❌ Sai: `https://internflow.vercel.app/`
❌ Sai: `http://internflow.vercel.app`

### 2. Credentials
- `allowCredentials(true)` cho phép gửi cookies/auth headers
- Frontend phải set `credentials: 'include'` khi fetch

```javascript
fetch(url, {
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json'
  }
})
```

### 3. Preflight Requests
- Browser tự động gửi OPTIONS request trước POST/PUT/DELETE
- Backend đã cấu hình sẵn để handle OPTIONS

## 🐛 Troubleshooting

### Lỗi: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Nguyên nhân**: URL frontend chưa được thêm vào `CORS_ALLOWED_ORIGINS`

**Giải pháp**:
1. Kiểm tra URL Vercel chính xác (copy từ Vercel Dashboard)
2. Thêm vào `CORS_ALLOWED_ORIGINS` trên Render
3. Save và đợi redeploy

### Lỗi: "CORS policy: The value of the 'Access-Control-Allow-Credentials' header"

**Nguyên nhân**: Frontend gửi `credentials: 'include'` nhưng backend không cho phép

**Giải pháp**: Đã fix sẵn trong code (`allowCredentials(true)`)

### Lỗi: "CORS policy: Method PUT is not allowed"

**Nguyên nhân**: Method không được cho phép

**Giải pháp**: Đã thêm sẵn tất cả methods: GET, POST, PUT, PATCH, DELETE, OPTIONS

## 📋 Checklist Deploy với CORS

- [ ] Đã cập nhật `CorsConfig.java` (đã làm sẵn)
- [ ] Đã thêm `CORS_ALLOWED_ORIGINS` vào `.env.render`
- [ ] Biết URL frontend Vercel chính xác
- [ ] URL có `https://` và không có `/` cuối
- [ ] Đã push code lên GitHub
- [ ] Deploy backend lên Render
- [ ] Test CORS từ frontend
- [ ] Kiểm tra Network tab trong DevTools

## 🔄 Workflow Deploy

1. **Deploy Backend** (Render):
   - Tạm thời để `CORS_ALLOWED_ORIGINS=https://placeholder.vercel.app`
   - Deploy và lấy backend URL

2. **Deploy Frontend** (Vercel):
   - Cập nhật API URL = backend URL từ bước 1
   - Deploy và lấy frontend URL

3. **Cập nhật CORS**:
   - Vào Render → Environment
   - Sửa `CORS_ALLOWED_ORIGINS` = frontend URL từ bước 2
   - Save → Redeploy

4. **Test**:
   - Mở frontend
   - Test các API calls
   - Kiểm tra không có CORS errors

## 💡 Tips

### Development
- Localhost luôn được cho phép
- Không cần cấu hình gì thêm cho local dev

### Production
- Chỉ thêm domain production vào `CORS_ALLOWED_ORIGINS`
- Không nên dùng wildcard `*` vì `allowCredentials(true)`

### Multiple Environments
```bash
# Staging + Production
CORS_ALLOWED_ORIGINS=https://internflow-staging.vercel.app,https://internflow.vercel.app
```

---

**Sau khi có URL Vercel, nhớ cập nhật biến `CORS_ALLOWED_ORIGINS` nhé!** 🚀
