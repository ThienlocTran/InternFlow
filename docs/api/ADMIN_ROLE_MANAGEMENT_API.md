# Admin Role Management API

## Mô tả
API này cho phép admin thay đổi role của user trong hệ thống. Admin có thể:
- **Thăng chức**: Nâng INTERN lên TEAM_LEADER
- **Giáng chức**: Hạ TEAM_LEADER xuống INTERN
- Thay đổi giữa các role: INTERN, TEAM_LEADER, MANAGER

## Endpoint

```
PATCH /api/admin/students/{userId}/role
```

## Request Parameters

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | UUID | Yes | ID của user cần thay đổi role |

### Request Body
```json
{
  "role": "TEAM_LEADER"
}
```

| Field | Type | Required | Description | Valid Values |
|-------|------|----------|-------------|--------------|
| role | String (Enum) | Yes | Role mới cho user | INTERN, TEAM_LEADER, MANAGER |

## Response Structure

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Cap nhat role thanh cong",
  "data": {
    "id": "uuid",
    "email": "student@example.com",
    "fullName": "Nguyen Van A",
    "role": "TEAM_LEADER",
    "studentCode": "SV001",
    "studentClass": "SE1801",
    "school": "FPT University",
    "phone": "0123456789",
    "cohort": {
      "id": "uuid",
      "name": "Khoa 2024",
      "startDate": "2024-01-01",
      "endDate": "2024-12-31"
    },
    "active": true,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

## Use Cases

### 1. Thăng chức INTERN lên TEAM_LEADER
```bash
curl -X PATCH "http://localhost:8080/api/admin/students/123e4567-e89b-12d3-a456-426614174000/role" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "role": "TEAM_LEADER"
  }'
```

**Khi nào sử dụng:**
- Sinh viên thể hiện khả năng lãnh đạo tốt
- Cần thêm team leader để quản lý nhóm
- Sinh viên được chọn làm trưởng nhóm

### 2. Giáng chức TEAM_LEADER xuống INTERN
```bash
curl -X PATCH "http://localhost:8080/api/admin/students/123e4567-e89b-12d3-a456-426614174000/role" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "role": "INTERN"
  }'
```

**Khi nào sử dụng:**
- Team leader không còn đảm nhiệm vai trò quản lý
- Sinh viên yêu cầu bỏ quyền team leader
- Tái cấu trúc team

### 3. Nâng lên MANAGER
```bash
curl -X PATCH "http://localhost:8080/api/admin/students/123e4567-e89b-12d3-a456-426614174000/role" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "role": "MANAGER"
  }'
```

**Khi nào sử dụng:**
- Cần người quản lý cấp cao hơn
- Sinh viên có trách nhiệm giám sát nhiều team

## Business Rules

### 1. Role Transitions (Chuyển đổi role hợp lệ)
✅ **Được phép:**
- INTERN → TEAM_LEADER
- INTERN → MANAGER
- TEAM_LEADER → INTERN
- TEAM_LEADER → MANAGER
- MANAGER → INTERN
- MANAGER → TEAM_LEADER

❌ **Không được phép:**
- Bất kỳ role nào → ADMIN (chỉ set qua email whitelist)
- ADMIN → bất kỳ role nào (không thể thay đổi role của admin)

### 2. Validation Rules
- User ID phải tồn tại trong hệ thống
- Role mới phải khác role hiện tại
- Không thể thay đổi role của user có role ADMIN
- Không thể set role thành ADMIN qua endpoint này

### 3. Security
- Chỉ user có role ADMIN mới có quyền gọi API này
- Endpoint nằm trong `/api/admin/*` path

## Error Responses

### 400 Bad Request - User ID không hợp lệ
```json
{
  "success": false,
  "message": "User id la bat buoc",
  "data": null
}
```

### 400 Bad Request - Role không hợp lệ
```json
{
  "success": false,
  "message": "Role moi la bat buoc",
  "data": null
}
```

### 400 Bad Request - User đã có role này
```json
{
  "success": false,
  "message": "User da co role nay roi",
  "data": null
}
```

### 400 Bad Request - Không thể thay đổi role của admin
```json
{
  "success": false,
  "message": "Khong the thay doi role cua admin",
  "data": null
}
```

### 400 Bad Request - Không thể set role thành ADMIN
```json
{
  "success": false,
  "message": "Khong the set role thanh ADMIN qua endpoint nay",
  "data": null
}
```

### 400 Bad Request - Chuyển đổi role không hợp lệ
```json
{
  "success": false,
  "message": "Khong the chuyen tu ADMIN sang INTERN",
  "data": null
}
```

### 404 Not Found - User không tồn tại
```json
{
  "success": false,
  "message": "Khong tim thay user",
  "data": null
}
```

## Implementation Details

### Service Layer
- **Method**: `UserService.updateRole(UUID userId, UserRole newRole)`
- **Transaction**: `@Transactional`
- **Validation**: 
  - Kiểm tra user tồn tại
  - Kiểm tra role hiện tại không phải ADMIN
  - Kiểm tra role mới không phải ADMIN
  - Kiểm tra role mới khác role hiện tại
  - Kiểm tra chuyển đổi role hợp lệ

### Controller Layer
- **Method**: `PATCH /api/admin/students/{userId}/role`
- **Request Body**: `UpdateUserRoleRequest`
- **Response**: `UserResponse`

## Testing Scenarios

### Scenario 1: Thăng chức thành công
```
Given: User có role INTERN
When: Admin gọi API với role = TEAM_LEADER
Then: User được cập nhật role thành TEAM_LEADER
```

### Scenario 2: Giáng chức thành công
```
Given: User có role TEAM_LEADER
When: Admin gọi API với role = INTERN
Then: User được cập nhật role thành INTERN
```

### Scenario 3: Không thể thay đổi role của admin
```
Given: User có role ADMIN
When: Admin gọi API với role = INTERN
Then: Trả về lỗi "Khong the thay doi role cua admin"
```

### Scenario 4: Không thể set role thành ADMIN
```
Given: User có role INTERN
When: Admin gọi API với role = ADMIN
Then: Trả về lỗi "Khong the set role thanh ADMIN qua endpoint nay"
```

### Scenario 5: Role không thay đổi
```
Given: User có role TEAM_LEADER
When: Admin gọi API với role = TEAM_LEADER
Then: Trả về lỗi "User da co role nay roi"
```

## Notes

1. **Admin Role Protection**: Role ADMIN chỉ có thể được set thông qua email whitelist trong code, không thể thay đổi qua API
2. **Audit Trail**: Mọi thay đổi role đều được ghi lại timestamp trong `updatedAt` field
3. **Team Impact**: Khi giáng chức TEAM_LEADER xuống INTERN, cần kiểm tra xem user có đang là leader của team nào không (có thể cần xử lý thêm)
4. **Permission Check**: Trong production, cần implement proper authentication và authorization để đảm bảo chỉ ADMIN mới gọi được API này

## Related APIs

- `GET /api/users/{id}` - Xem thông tin user
- `GET /api/users` - Lấy danh sách tất cả users
- `GET /api/admin/students/{studentId}/detail` - Xem chi tiết sinh viên (admin)
- `POST /api/teams` - Tạo team (yêu cầu TEAM_LEADER role)

## Future Enhancements

1. **Role History**: Lưu lịch sử thay đổi role
2. **Notification**: Thông báo cho user khi role thay đổi
3. **Team Validation**: Kiểm tra và xử lý khi giáng chức team leader đang quản lý team
4. **Bulk Update**: Cho phép thay đổi role nhiều users cùng lúc
5. **Role Permissions**: Định nghĩa chi tiết permissions cho từng role
