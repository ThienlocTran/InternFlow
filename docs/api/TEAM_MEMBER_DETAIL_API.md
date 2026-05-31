# Team Member Detail API

## Mô tả
API này cho phép team leader xem chi tiết thông tin của từng thành viên trong team, bao gồm:
- Thông tin ca đăng ký (schedule registrations)
- Lịch điểm danh (attendance records) với trạng thái ảnh
- Tổng hợp nhật ký thực tập (report journal summary)

Team leader có thể biết được:
- Thành viên nào đang thiếu ảnh điểm danh (checkin/checkout timemark và group images)
- Thành viên nào viết bài nhật ký thiếu trang
- Tổng số trang đã viết
- Số ngày có/không có báo cáo

## Endpoint

```
GET /api/teams/member-detail
```

## Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| leaderId | UUID | Yes | ID của team leader |
| memberId | UUID | Yes | ID của thành viên cần xem chi tiết |
| startDate | LocalDate | No | Ngày bắt đầu (format: YYYY-MM-DD). Mặc định: 30 ngày trước |
| endDate | LocalDate | No | Ngày kết thúc (format: YYYY-MM-DD). Mặc định: hôm nay |

## Response Structure

```json
{
  "success": true,
  "message": "Lay chi tiet thanh vien thanh cong",
  "data": {
    "user": {
      "id": "uuid",
      "email": "string",
      "fullName": "string",
      "role": "STUDENT",
      "studentCode": "string",
      "avatarUrl": "string"
    },
    "scheduleRegistrations": [
      {
        "id": "uuid",
        "user": {...},
        "shift": {
          "id": "uuid",
          "name": "string",
          "startTime": "HH:mm:ss",
          "endTime": "HH:mm:ss"
        },
        "scheduleDate": "YYYY-MM-DD",
        "status": "REGISTERED",
        "note": "string",
        "createdAt": "timestamp"
      }
    ],
    "attendanceRecords": [
      {
        "id": "uuid",
        "shift": {...},
        "attendanceDate": "YYYY-MM-DD",
        "status": "PRESENT",
        "checkinTime": "timestamp",
        "checkoutTime": "timestamp",
        "hasCheckinTimemarkImage": true,
        "hasCheckinGroupImage": true,
        "hasCheckoutTimemarkImage": false,
        "hasCheckoutGroupImage": false,
        "missingImageCount": 2,
        "reportPageCount": 5,
        "hasReportDocument": true,
        "note": "string"
      }
    ],
    "reportJournalSummary": {
      "totalPagesWritten": 150,
      "totalDaysWithReport": 25,
      "totalDaysWithoutReport": 5,
      "lastReportDate": "YYYY-MM-DD"
    }
  }
}
```

## Response Fields Explanation

### attendanceRecords
- `hasCheckinTimemarkImage`: Có ảnh chấm công checkin không
- `hasCheckinGroupImage`: Có ảnh nhóm checkin không
- `hasCheckoutTimemarkImage`: Có ảnh chấm công checkout không
- `hasCheckoutGroupImage`: Có ảnh nhóm checkout không
- `missingImageCount`: Số lượng ảnh còn thiếu (tối đa 4)
- `reportPageCount`: Số trang báo cáo đã viết trong ngày đó
- `hasReportDocument`: Có tài liệu báo cáo không

### reportJournalSummary
- `totalPagesWritten`: Tổng số trang đã viết trong khoảng thời gian
- `totalDaysWithReport`: Số ngày có viết báo cáo
- `totalDaysWithoutReport`: Số ngày không viết báo cáo
- `lastReportDate`: Ngày viết báo cáo gần nhất

## Example Request

```bash
curl -X GET "http://localhost:8080/api/teams/member-detail?leaderId=123e4567-e89b-12d3-a456-426614174000&memberId=123e4567-e89b-12d3-a456-426614174001&startDate=2026-05-01&endDate=2026-05-18" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Business Rules

1. Chỉ user có role `TEAM_LEADER` mới có thể gọi API này
2. Nếu không truyền `startDate` và `endDate`, hệ thống sẽ lấy dữ liệu 30 ngày gần nhất
3. Attendance records được sắp xếp theo ngày giảm dần, sau đó theo giờ bắt đầu ca
4. Schedule registrations được sắp xếp theo ngày tăng dần, sau đó theo giờ bắt đầu ca

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Leader id va member id la bat buoc",
  "data": null
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Chi nhom truong moi xem duoc chi tiet thanh vien",
  "data": null
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Khong tim thay user",
  "data": null
}
```

## Use Cases

### 1. Kiểm tra thành viên thiếu ảnh điểm danh
Team leader có thể xem `missingImageCount` trong `attendanceRecords` để biết thành viên nào thiếu ảnh.

### 2. Kiểm tra thành viên viết báo cáo thiếu
Team leader có thể xem:
- `reportPageCount` trong từng attendance record
- `totalDaysWithoutReport` trong summary để biết có bao nhiêu ngày không viết báo cáo

### 3. Theo dõi tiến độ thực tập
Team leader có thể xem `totalPagesWritten` và `lastReportDate` để đánh giá mức độ tích cực của thành viên.

## Notes

- API này yêu cầu authentication (JWT token)
- Dữ liệu được lấy theo khoảng thời gian để tránh load quá nhiều dữ liệu
- Mặc định lấy 30 ngày gần nhất nếu không chỉ định date range
