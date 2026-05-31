# Tính năng Quản lý Role cho Admin

## Tổng quan
Admin có thể thay đổi role của user trong hệ thống để thăng/giáng chức.

## Các Role trong hệ thống
1. **INTERN** - Sinh viên thực tập thông thường
2. **TEAM_LEADER** - Trưởng nhóm, có quyền quản lý team
3. **ADMIN** - Quản trị viên hệ thống (không thể thay đổi qua API)

## API Endpoint
```
PATCH /api/admin/students/{userId}/role
```

### Request Body
```json
{
  "role": "TEAM_LEADER"
}
```

### Response
```json
{
  "success": true,
  "message": "Cap nhat role thanh cong",
  "data": {
    "id": "uuid",
    "email": "student@example.com",
    "fullName": "Nguyen Van A",
    "role": "TEAM_LEADER",
    ...
  }
}
```

## Use Cases

### 1. Thăng chức INTERN lên TEAM_LEADER
```bash
PATCH /api/admin/students/{userId}/role
Body: { "role": "TEAM_LEADER" }
```
**Khi nào:** Sinh viên được chọn làm trưởng nhóm

### 2. Giáng chức TEAM_LEADER xuống INTERN
```bash
PATCH /api/admin/students/{userId}/role
Body: { "role": "INTERN" }
```
**Khi nào:** Team leader không còn đảm nhiệm vai trò quản lý

## Quy tắc

### ✅ Được phép
- INTERN ↔ TEAM_LEADER

### ❌ Không được phép
- Thay đổi role của ADMIN
- Set role thành ADMIN (chỉ qua email whitelist)
- Set role giống role hiện tại

## Files liên quan

### Backend
- `UpdateUserRoleRequest.java` - DTO request
- `UserService.java` - Interface
- `UserServiceImpl.java` - Implementation với validation
- `AdminStudentController.java` - Controller với endpoint

### Documentation
- `../api/ADMIN_ROLE_MANAGEMENT_API.md` - Chi tiết API đầy đủ
- `../api/ADMIN_ROLE_MANAGEMENT_API.md` - API details

## Testing

### Test Cases
1. ✅ Thăng chức INTERN → TEAM_LEADER thành công
2. ✅ Giáng chức TEAM_LEADER → INTERN thành công
3. ❌ Không thể thay đổi role của ADMIN
4. ❌ Không thể set role thành ADMIN
5. ❌ Không thể set role giống role hiện tại

### Manual Testing
```bash
# 1. Thăng chức
curl -X PATCH "http://localhost:8080/api/admin/students/{userId}/role" \
  -H "Content-Type: application/json" \
  -d '{"role": "TEAM_LEADER"}'

# 2. Giáng chức
curl -X PATCH "http://localhost:8080/api/admin/students/{userId}/role" \
  -H "Content-Type: application/json" \
  -d '{"role": "INTERN"}'
```

## Lưu ý quan trọng

1. **Security**: Trong production, cần implement authentication/authorization để chỉ ADMIN mới gọi được API
2. **Team Impact**: Khi giáng chức TEAM_LEADER, cần kiểm tra xem họ có đang lead team nào không
3. **Audit**: Mọi thay đổi role đều được ghi timestamp trong `updated_at`
4. **Notification**: Có thể cần thông báo cho user khi role thay đổi

## Future Enhancements
- [ ] Lưu lịch sử thay đổi role
- [ ] Gửi notification khi role thay đổi
- [ ] Validate và xử lý team leadership khi giáng chức
- [ ] Bulk update roles cho nhiều users
- [ ] Role permissions chi tiết hơn
