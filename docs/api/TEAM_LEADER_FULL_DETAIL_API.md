# Team Leader - Member Full Detail API

## Mô tả
API này cho phép team leader xem **CHI TIẾT ĐẦY ĐỦ** thông tin của sinh viên trong một ngày cụ thể, bao gồm:
- ✅ Lịch đăng ký ca (schedule registrations)
- ✅ Điểm danh đầy đủ với **TẤT CẢ HÌNH ẢNH** (checkin/checkout timemark, group images, và attendance images)
- ✅ Nhật ký thực tập đầy đủ (report journal entries)

**Đặc điểm:**
- **TÁI SỬ DỤNG** các service và DTO đã có sẵn (AttendanceService, ReportJournalService)
- **KHÔNG CÓ CODE LẶP** - sử dụng AttendanceResponse và DailyReportEntryResponse có sẵn
- Team leader có thể thấy rõ sinh viên chụp thiếu ảnh nào, viết nhật ký thiếu hay không

## Endpoint

```
GET /api/teams/member-full-detail
```

## Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| leaderId | UUID | Yes | ID của team leader |
| memberId | UUID | Yes | ID của sinh viên cần xem chi tiết |
| date | LocalDate | No | Ngày cần xem (format: YYYY-MM-DD). Mặc định: hôm nay |

## Response Structure

```json
{
  "success": true,
  "message": "Lay chi tiet day du thanh vien thanh cong",
  "data": {
    "user": {
      "id": "uuid",
      "email": "student@example.com",
      "fullName": "Nguyen Van A",
      "role": "INTERN",
      "studentCode": "SV001",
      "avatarUrl": "https://..."
    },
    "date": "2026-05-18",
    "scheduleRegistrations": [
      {
        "id": "uuid",
        "user": {...},
        "shift": {
          "id": "uuid",
          "name": "Ca 1",
          "code": "SHIFT_1",
          "startTime": "08:00:00",
          "endTime": "12:00:00",
          "maxParticipants": 30
        },
        "scheduleDate": "2026-05-18",
        "status": "REGISTERED",
        "note": null,
        "createdAt": "2026-05-15T10:00:00Z"
      }
    ],
    "attendances": [
      {
        "id": "uuid",
        "user": {...},
        "shift": {...},
        "attendanceDate": "2026-05-18",
        "status": "CHECKED_OUT",
        "checkinTime": "2026-05-18T08:00:00Z",
        "checkoutTime": "2026-05-18T12:00:00Z",
        "checkinTimemarkImageUrl": "https://storage.../checkin-timemark.jpg",
        "checkinGroupImageUrl": "https://storage.../checkin-group.jpg",
        "checkoutTimemarkImageUrl": "https://storage.../checkout-timemark.jpg",
        "checkoutGroupImageUrl": "https://storage.../checkout-group.jpg",
        "note": "Diem danh binh thuong",
        "reportPageCount": 8,
        "reportDocumentUrl": "https://storage.../report.docx",
        "images": [
          {
            "id": "uuid",
            "attendanceId": "uuid",
            "imageType": "WORK_PROGRESS",
            "phase": "DURING_SHIFT",
            "expectedTime": "10:00:00",
            "imageUrl": "https://storage.../work-progress-1.jpg",
            "displayOrder": 1,
            "note": "Dang lam viec",
            "uploadedAt": "2026-05-18T10:05:00Z"
          },
          {
            "id": "uuid",
            "attendanceId": "uuid",
            "imageType": "WORK_PROGRESS",
            "phase": "DURING_SHIFT",
            "expectedTime": "11:00:00",
            "imageUrl": "https://storage.../work-progress-2.jpg",
            "displayOrder": 2,
            "note": "Tiep tuc lam viec",
            "uploadedAt": "2026-05-18T11:05:00Z"
          }
        ]
      }
    ],
    "reportEntries": [
      {
        "document": {
          "id": "uuid",
          "user": {...},
          "title": "Nhat ky thuc tap",
          "totalPages": 150,
          "completedShiftCount": 25,
          "currentFileName": "Nhat ky thuc tap- duoc 25 ca -Nguyen Van A"
        },
        "entry": {
          "id": "uuid",
          "workDate": "2026-05-18",
          "content": "Hom nay toi da hoc duoc...",
          "referenceLinks": "https://docs.example.com",
          "shiftCodes": "Ca 1",
          "shiftCount": 1,
          "workTimeSummary": "Thoi gian lam viec tu 08:00 - 12:00",
          "pageCount": 8,
          "requiredPages": 8,
          "status": "READY_FOR_MAIL",
          "createdAt": "2026-05-18T08:00:00Z",
          "updatedAt": "2026-05-18T12:30:00Z"
        }
      }
    ]
  }
}
```

## Response Fields Explanation

### scheduleRegistrations
Danh sách các ca đã đăng ký trong ngày

### attendances
**Thông tin điểm danh đầy đủ** bao gồm:
- `checkinTimemarkImageUrl`: Ảnh chấm công vào ca
- `checkinGroupImageUrl`: Ảnh nhóm vào ca
- `checkoutTimemarkImageUrl`: Ảnh chấm công tan ca
- `checkoutGroupImageUrl`: Ảnh nhóm tan ca
- `images[]`: **Danh sách TẤT CẢ ảnh bổ sung** (work progress, breaks, etc.)
  - `imageType`: Loại ảnh (WORK_PROGRESS, BREAK, MEETING, etc.)
  - `phase`: Giai đoạn (DURING_SHIFT, BEFORE_SHIFT, AFTER_SHIFT)
  - `expectedTime`: Thời gian dự kiến chụp
  - `imageUrl`: URL ảnh
  - `displayOrder`: Thứ tự hiển thị
- `reportPageCount`: Số trang báo cáo đã viết
- `reportDocumentUrl`: Link tài liệu báo cáo

### reportEntries
**Nhật ký thực tập đầy đủ** bao gồm:
- `content`: Nội dung nhật ký
- `referenceLinks`: Tài liệu tham khảo
- `pageCount`: Số trang đã viết
- `requiredPages`: Số trang yêu cầu
- `status`: Trạng thái (READY_FOR_MAIL, NEEDS_MORE_PAGES)

## Example Request

```bash
# Xem chi tiết ngày hôm nay
curl -X GET "http://localhost:8080/api/teams/member-full-detail?leaderId=123e4567-e89b-12d3-a456-426614174000&memberId=123e4567-e89b-12d3-a456-426614174001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Xem chi tiết ngày cụ thể
curl -X GET "http://localhost:8080/api/teams/member-full-detail?leaderId=123e4567-e89b-12d3-a456-426614174000&memberId=123e4567-e89b-12d3-a456-426614174001&date=2026-05-15" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Use Cases

### 1. Kiểm tra sinh viên chụp thiếu ảnh
Team leader xem `attendances[].images[]` để kiểm tra:
- Có đủ 4 ảnh bắt buộc không (checkin/checkout timemark + group)?
- Có ảnh work progress trong ca không?
- Ảnh nào còn thiếu?

**Ví dụ:**
```javascript
// Kiểm tra ảnh thiếu
const attendance = data.attendances[0];
const missingImages = [];

if (!attendance.checkinTimemarkImageUrl) missingImages.push("Checkin Timemark");
if (!attendance.checkinGroupImageUrl) missingImages.push("Checkin Group");
if (!attendance.checkoutTimemarkImageUrl) missingImages.push("Checkout Timemark");
if (!attendance.checkoutGroupImageUrl) missingImages.push("Checkout Group");

console.log("Thiếu ảnh:", missingImages);
console.log("Số ảnh bổ sung:", attendance.images.length);
```

### 2. Kiểm tra nhật ký viết thiếu
Team leader xem `reportEntries[]` để kiểm tra:
- Có viết nhật ký không?
- Số trang có đủ yêu cầu không?
- Nội dung có đầy đủ không?

**Ví dụ:**
```javascript
const entry = data.reportEntries[0]?.entry;
if (!entry) {
  console.log("Chưa viết nhật ký");
} else if (entry.pageCount < entry.requiredPages) {
  console.log(`Thiếu ${entry.requiredPages - entry.pageCount} trang`);
} else {
  console.log("Đã viết đủ nhật ký");
}
```

### 3. Xem toàn bộ hoạt động trong ngày
Team leader có thể xem:
- Sinh viên đăng ký ca nào
- Đã checkin/checkout chưa
- Chụp ảnh đầy đủ chưa
- Viết nhật ký đủ chưa

## Business Rules

1. Chỉ user có role `TEAM_LEADER` mới có thể gọi API này
2. Nếu không truyền `date`, hệ thống lấy dữ liệu ngày hôm nay
3. API **TÁI SỬ DỤNG** các service đã có:
   - `AttendanceService.getUserAttendances()` - lấy attendance với images
   - `ReportJournalService.getEntriesByDate()` - lấy nhật ký theo ngày
4. Response sử dụng các DTO đã có sẵn:
   - `AttendanceResponse` (có sẵn field `images[]`)
   - `DailyReportEntryResponse`
   - `ScheduleRegistrationResponse`

## So sánh với API khác

### `/api/teams/member-detail` (API cũ)
- ✅ Xem tổng quan nhiều ngày (date range)
- ✅ Có summary về số ảnh thiếu, số trang viết
- ❌ **KHÔNG CÓ** URL ảnh chi tiết
- ❌ **KHÔNG CÓ** nội dung nhật ký

### `/api/teams/member-full-detail` (API mới)
- ✅ Xem chi tiết **MỘT NGÀY** cụ thể
- ✅ Có **TẤT CẢ URL ảnh** (checkin, checkout, work progress, etc.)
- ✅ Có **NỘI DUNG NHẬT KÝ ĐẦY ĐỦ**
- ✅ Team leader thấy rõ thiếu ảnh nào, viết thiếu gì

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

## Technical Implementation

### Code Reuse (Không có code lặp)
```java
// Tái sử dụng AttendanceService
List<AttendanceResponse> attendances = attendanceService.getUserAttendances(memberId, targetDate);

// Tái sử dụng ReportJournalService
List<DailyReportEntryResponse> reportEntries = reportJournalService.getEntriesByDate(targetDate)
    .stream()
    .filter(entry -> entry.document().user().id().equals(memberId))
    .toList();
```

### Benefits
1. ✅ **Không có code lặp** - tái sử dụng service đã có
2. ✅ **Consistent data** - dữ liệu giống với API gốc
3. ✅ **Easy maintenance** - chỉ cần maintain ở một chỗ
4. ✅ **Full detail** - team leader thấy đầy đủ thông tin

## Notes

1. **Performance**: API này lấy dữ liệu chi tiết nên có thể chậm hơn API summary
2. **Use case**: Dùng khi team leader cần xem chi tiết một ngày cụ thể
3. **Images**: Tất cả URL ảnh đều được trả về, team leader có thể xem từng ảnh
4. **Report content**: Nội dung nhật ký đầy đủ được trả về để team leader review

## Related APIs

- `GET /api/teams/member-detail` - Xem tổng quan nhiều ngày (summary)
- `GET /api/attendances` - Xem attendance của chính mình
- `GET /api/report-journals/daily` - Xem nhật ký theo ngày (tất cả users)
