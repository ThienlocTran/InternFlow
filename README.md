# InternFlow Backend

Backend cho hệ thống quản lý thực tập `InternFlow`, xây dựng bằng Spring Boot để phục vụ các nghiệp vụ:

- Đăng nhập bằng Google.
- Quản lý hồ sơ sinh viên thực tập.
- Quản lý khóa thực tập, team, team leader.
- Đăng ký ca làm.
- Check-in / check-out và lưu ảnh điểm danh.
- Viết nhật ký thực tập theo ngày.
- Tạo file Word nhật ký và gửi mail cuối ngày bằng Gmail của sinh viên.
- Theo dõi tiến độ thực tập, bonus ca đêm và bonus leader.

README này tập trung mô tả đúng theo source hiện có trong `D:\CheckSV\InternFlow`.

## 1. Tổng quan kỹ thuật

### Stack chính

- Java 17
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Spring OAuth2 Client
- Spring Mail
- Apache POI (`.docx`)
- Cloudinary API
- Maven Wrapper

### Kiểu kiến trúc

Project đi theo mô hình quen thuộc:

- `controller`: nhận request HTTP, trả `ApiResponse`.
- `service`: xử lý nghiệp vụ chính.
- `repository`: truy cập database qua JPA.
- `entity`: mô hình dữ liệu.
- `dto/request`, `dto/response`: contract vào/ra cho API.
- `config`: cấu hình app, seed dữ liệu, CORS, scheduling, security.
- `scheduler`: job keep-alive cho Render free tier.
- `handle`, `exception`: xử lý lỗi tập trung.

### Đặc điểm quan trọng của source hiện tại

- Tất cả API dưới `/api/**` hiện đang `permitAll()` trong [SecurityConfig](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\config\SecurityConfig.java:1).
- Xác thực người dùng hiện tại chủ yếu dựa trên luồng `POST /api/auth/google`, backend verify Google `idToken` rồi tạo/tìm `AppUser`.
- Phân quyền chưa được enforce ở Spring Security filter level; logic role đang nằm ở dữ liệu và nghiệp vụ.
- Database hiện dùng `spring.jpa.hibernate.ddl-auto=update`, nghĩa là schema được Hibernate tự cập nhật.
- Thư mục `db/migrations/` đang chứa các script SQL phục vụ đồng bộ/thay đổi thủ công, chưa phải một hệ migration tự động như Flyway/Liquibase.

## 2. Bài toán nghiệp vụ backend đang giải quyết

### 2.1 Người dùng và vai trò

Hệ thống có các vai trò chính:

- `INTERN`
- `TEAM_LEADER`
- `MANAGER`
- `ADMIN`

Vai trò ảnh hưởng trực tiếp tới:

- Số ca tối đa trong ngày.
- Chỉ tiêu ca mỗi tuần.
- Yêu cầu số ca công ty / ca nhà.
- Bonus ca đêm.
- Bonus leadership.

Các rule này được cấu hình trong bảng `role_policies` và seed ở [DataInitializer.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\config\DataInitializer.java:1).

### 2.2 Luồng chính của sinh viên

1. Sinh viên đăng nhập Google.
2. Backend tạo user mới nếu chưa tồn tại.
3. Sinh viên tạo/cập nhật profile.
4. Sinh viên đăng ký ca làm trong tuần.
5. Tới ca làm, sinh viên check-in bằng ảnh TimeMark và có thể kèm ảnh nhóm.
6. Trong ca có thể lưu thêm các ảnh audit.
7. Cuối ca sinh viên checkout.
8. Sinh viên viết nhật ký cho ngày làm việc.
9. Khi đủ số trang tối thiểu, sinh viên gửi mail cuối ngày.
10. Backend tạo file Word nhật ký, gom ảnh điểm danh, gửi qua Gmail API bằng chính tài khoản của sinh viên.

### 2.3 Luồng của team leader / admin

- `TEAM_LEADER` có thể xem peer cùng ca, xem chi tiết thành viên.
- `ADMIN` có thể xem chi tiết sinh viên và cập nhật role user.
- Hệ thống có cohort để gom sinh viên theo khóa thực tập.

## 3. Cấu trúc thư mục

```text
InternFlow/
|-- src/main/java/com/java6/springboot/internflow
|   |-- config/
|   |-- controller/
|   |-- dto/
|   |-- entity/
|   |-- enums/
|   |-- exception/
|   |-- handle/
|   |-- repository/
|   |-- scheduler/
|   |-- service/
|   `-- InternFlowApplication.java
|-- src/main/resources/
|   |-- application.properties
|   `-- application-production.properties
|-- src/test/java/
|-- db/migrations/
|-- Dockerfile
|-- render.yaml
|-- test-build.cmd
|-- test-build.sh
`-- pom.xml
```

## 4. Domain model chính

Theo các entity hiện có, hệ thống xoay quanh các nhóm dữ liệu:

### Nhân sự

- `AppUser`: người dùng trung tâm.
- `InternshipCohort`: khóa thực tập.
- `Team`: team làm việc.
- `TeamMember`: thành viên team.
- `RolePolicy`: rule theo vai trò.

### Lịch và điểm danh

- `Shift`: định nghĩa ca.
- `ScheduleRegistration`: đăng ký ca theo ngày.
- `Attendance`: bản ghi check-in/check-out.
- `AttendanceImage`: ảnh bổ sung trong ca.

### Báo cáo và email

- `ReportDocument`: document tổng của 1 user.
- `ReportEntry`: nhật ký của 1 ngày.
- `ReportRevision`: lịch sử chỉnh sửa nhật ký.
- `EmailLog`: log gửi mail cuối ngày.

Có thể xem ERD chi tiết trong [DB_ERD.md](D:\CheckSV\InternFlow\DB_ERD.md).

## 5. Các rule nghiệp vụ nổi bật trong code

### 5.1 Seed dữ liệu khi app khởi động

`DataInitializer` hiện tự seed:

- 4 ca mặc định `SHIFT_1` đến `SHIFT_4`.
- Policy mặc định cho `INTERN`, `TEAM_LEADER`, `MANAGER`, `ADMIN`.
- 2 email admin cố định.
- 1 email được ép vai trò `TEAM_LEADER` nếu đã tồn tại user.

Điểm cần lưu ý:

- Nếu môi trường production dùng dữ liệu thật, logic seed này vẫn chạy mỗi lần app start.
- Seed đang mang cả dữ liệu hệ thống lẫn dữ liệu tài khoản cụ thể.

### 5.2 Đăng ký ca

Trong [ScheduleRegistrationServiceImpl.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\service\impl\ScheduleRegistrationServiceImpl.java:1), các rule chính là:

- Không được đăng ký ngày trong quá khứ.
- Phải chọn ít nhất 1 ca.
- Không vượt `maxShiftsPerDay`.
- Không vượt quota tích lũy theo tuần.
- Nên chọn các ca liền kề nhau trong cùng ngày.
- `TEAM_LEADER` không chiếm slot `maxParticipants`.
- `INTERN` bị giới hạn bởi sức chứa ca.
- Không được đăng ký trùng một ca trong cùng ngày.
- Có thể hủy đăng ký nếu ca chưa qua ngày và chưa phát sinh attendance.

### 5.3 Điểm danh

Trong [AttendanceServiceImpl.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\service\impl\AttendanceServiceImpl.java:1):

- Muốn check-in phải đăng ký ca trước.
- Mỗi `user + shift + date` chỉ có 1 attendance.
- Role không có quota thực tập thì không được điểm danh ca.
- Check-in bắt buộc có ảnh TimeMark.
- Ảnh nhóm là tùy chọn.
- Checkout chỉ thực hiện khi trạng thái đang là `CHECKED_IN`.
- Có endpoint lưu draft ảnh checkout trước khi chốt checkout.
- Có thể lưu thêm ảnh audit trong ca theo `imageType + phase + expectedTime`.

### 5.4 Nhật ký thực tập

Trong [ReportJournalServiceImpl.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\service\impl\ReportJournalServiceImpl.java:1):

- Chỉ viết nhật ký khi ngày đó đã có đăng ký ca.
- Mỗi ngày có 1 `ReportEntry`.
- Khi save nội dung sẽ:
  - Ước lượng số trang từ số từ.
  - Tính số trang tối thiểu bắt buộc.
  - Lưu lịch sử revision.
  - Cập nhật document tổng.
- Nếu ngày có `SHIFT_1` hoặc `SHIFT_2` thì yêu cầu `8` trang ước tính.
- Nếu chỉ có ca tối thì yêu cầu `5` trang ước tính.

### 5.5 Tính tiến độ và bonus

Trong [InternshipProgressCalculator.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\service\impl\InternshipProgressCalculator.java:1):

- Chỉ tính các attendance đã `CHECKED_OUT`.
- Chỉ tính ca có `ShiftCategory.COMPANY`.
- Có bonus ca đêm theo threshold.
- Có bonus leadership theo threshold.
- `completedShiftCount` hiển thị trong report document là số ca hiệu dụng sau bonus.

### 5.6 Gửi mail cuối ngày

Source hiện đang gửi mail bằng Gmail API, không phải SMTP gửi trực tiếp từ backend:

- FE phải cấp `googleAccessToken`.
- Backend verify token email trùng với user đang đăng nhập.
- Backend build file `.docx` bằng Apache POI.
- Backend tải ảnh điểm danh từ URL Cloudinary để đính kèm mail.
- Có giới hạn:
  - mỗi ảnh tối đa `8MB`
  - tổng attachment tối đa `20MB`
- Kết quả gửi mail được lưu vào `email_logs`.

## 6. Danh sách API chính

Tất cả response thành công/không thành công đều theo wrapper:

```json
{
  "success": true,
  "message": "string",
  "data": {},
  "timestamp": "2026-05-26T00:00:00Z"
}
```

### Auth

- `POST /api/auth/google`

### Health

- `GET /api/health`
- `GET /api/ping`

### User

- `POST /api/users/profile`
- `PUT /api/users/{id}/profile`
- `GET /api/users/{id}`
- `GET /api/users`

### Cohort

- `POST /api/cohorts`
- `GET /api/cohorts`
- `GET /api/cohorts/{cohortId}/students`
- `GET /api/cohorts/students/{studentId}`

### Team

- `POST /api/teams`
- `POST /api/teams/{teamId}/members`
- `GET /api/teams/{teamId}/members`
- `GET /api/teams/leader-shift-peers`
- `GET /api/teams/member-detail`
- `GET /api/teams/member-full-detail`

### Shift

- `GET /api/shifts`

### Schedule

- `POST /api/schedules`
- `GET /api/schedules`
- `GET /api/schedules/capacity`
- `DELETE /api/schedules/{registrationId}`

### Attendance

- `GET /api/attendances`
- `POST /api/attendances/checkin`
- `POST /api/attendances/{attendanceId}/checkout`
- `POST /api/attendances/{attendanceId}/checkout-draft`
- `POST /api/attendances/{attendanceId}/images`
- `GET /api/attendances/{attendanceId}/images`

### Report journal

- `GET /api/report-journals`
- `GET /api/report-journals/daily`
- `PUT /api/report-journals/entries`
- `GET /api/report-journals/entries/{entryId}/revisions`
- `POST /api/report-journals/submit-mail`
- `GET /api/report-journals/email-logs`

### Admin

- `GET /api/admin/students/{studentId}/detail`
- `PATCH /api/admin/students/{userId}/role`

### Upload

- `POST /api/uploads/images`

## 7. Cấu hình môi trường

### 7.1 Profile local

File [application.properties](D:\CheckSV\InternFlow\src\main\resources\application.properties:1) là cấu hình mặc định.

Các biến môi trường chính:

| Biến | Bắt buộc | Mục đích |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Không | `local` hoặc `production` |
| `DATABASE_URL` | Có | JDBC URL PostgreSQL |
| `DATABASE_USERNAME` | Có | Username DB |
| `DATABASE_PASSWORD` | Có | Password DB |
| `CLOUDINARY_CLOUD_NAME` | Có nếu upload ảnh | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Có nếu upload ảnh | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Có nếu upload ảnh | Cloudinary API secret |
| `GOOGLE_OAUTH_CLIENT_ID` | Có | Verify Google ID token |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Tùy chọn | Phục vụ OAuth client config |
| `MAIL_HOST` | Tùy chọn | SMTP host, hiện chủ yếu để giữ cấu hình mail |
| `MAIL_PORT` | Tùy chọn | SMTP port |
| `MAIL_USERNAME` | Tùy chọn | SMTP account |
| `MAIL_PASSWORD` | Tùy chọn | SMTP password/app password |
| `INTERNFLOW_MAIL_TO` | Có nếu gửi mail | Người nhận mail báo cáo |
| `INTERNFLOW_MAIL_CC` | Tùy chọn | Danh sách CC |
| `APP_BASE_URL` | Có khi deploy Render free | URL backend để keep-alive ping chính nó |
| `KEEP_ALIVE_ENABLED` | Không | Bật/tắt keep-alive scheduler |
| `KEEP_ALIVE_ENDPOINT` | Không | Mặc định `/api/health` |
| `KEEP_ALIVE_INITIAL_DELAY_MS` | Không | Delay ping đầu tiên |
| `KEEP_ALIVE_INTERVAL_MS` | Không | Chu kỳ ping |
| `CORS_ALLOWED_ORIGINS` | Có khi deploy | Danh sách domain frontend, phân tách bằng dấu phẩy |
| `MAX_UPLOAD_FILE_SIZE` | Không | Giới hạn file upload |
| `MAX_UPLOAD_REQUEST_SIZE` | Không | Giới hạn request upload |

### 7.2 Profile production

File [application-production.properties](D:\CheckSV\InternFlow\src\main\resources\application-production.properties:1) bổ sung:

- Hikari pool cho production.
- `server.port=${PORT:8080}`.
- bật nén response.
- CORS domain từ env.
- log level production.

## 8. CORS và bảo mật

### CORS

Trong [CorsConfig.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\config\CorsConfig.java:1):

- Localhost `5173` và `5174` luôn được allow.
- Có thể thêm domain bằng `CORS_ALLOWED_ORIGINS`.
- Áp dụng cho `/api/**`.

### Security hiện tại

Trong [SecurityConfig.java](D:\CheckSV\InternFlow\src\main\java\com\java6\springboot\internflow\config\SecurityConfig.java:1):

- CSRF disabled.
- Chưa có JWT/session auth cho API business.
- Tất cả API đang mở.

Khuyến nghị cho các phase tiếp theo:

- Thêm JWT hoặc session-based auth chuẩn.
- Enforce authorization theo role ở backend thay vì dựa vào frontend.
- Giới hạn các endpoint admin/team leader bằng role guard.

## 9. Quy trình chạy local

### Điều kiện cần

- Cài JDK 17
- Có PostgreSQL
- Có tài khoản Cloudinary
- Có Google OAuth Client ID

### Bước 1: cấu hình database

Tạo database ví dụ:

```sql
CREATE DATABASE internflow;
```

### Bước 2: set environment variables

Ví dụ PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/internflow"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="postgres"
$env:CLOUDINARY_CLOUD_NAME="your-cloud-name"
$env:CLOUDINARY_API_KEY="your-api-key"
$env:CLOUDINARY_API_SECRET="your-api-secret"
$env:GOOGLE_OAUTH_CLIENT_ID="your-google-client-id"
$env:INTERNFLOW_MAIL_TO="receiver@example.com"
$env:INTERNFLOW_MAIL_CC="cc@example.com"
```

### Bước 3: chạy project

```powershell
.\mvnw spring-boot:run
```

Hoặc build jar:

```powershell
.\mvnw clean package
java -jar target\InternFlow-0.0.1-SNAPSHOT.jar
```

### Bước 4: test nhanh

```powershell
curl http://localhost:8080/api/health
```

## 10. Quy trình nghiệp vụ end-to-end đề xuất cho team dev/test

### Luồng onboarding user mới

1. Đăng nhập qua `POST /api/auth/google`.
2. Nếu user mới, backend tự tạo `AppUser`.
3. Dùng `POST /api/users/profile` để hoàn thiện hồ sơ.
4. Nếu cần gán cohort, team hoặc role đặc biệt thì thao tác thêm qua các API quản trị.

### Luồng đăng ký ca và điểm danh

1. Lấy danh sách ca qua `GET /api/shifts`.
2. Xem sức chứa qua `GET /api/schedules/capacity`.
3. Đăng ký ca qua `POST /api/schedules`.
4. Upload ảnh qua `POST /api/uploads/images`.
5. Check-in qua `POST /api/attendances/checkin`.
6. Nếu cần, thêm ảnh trong ca qua `POST /api/attendances/{attendanceId}/images`.
7. Lưu ảnh checkout tạm qua `POST /api/attendances/{attendanceId}/checkout-draft`.
8. Checkout chính thức qua `POST /api/attendances/{attendanceId}/checkout`.

### Luồng viết nhật ký và gửi mail

1. Gọi `GET /api/report-journals?userId=...` để lấy progress.
2. Lưu nội dung nhật ký qua `PUT /api/report-journals/entries`.
3. Kiểm tra số trang ước tính đã đủ chưa.
4. Xin `googleAccessToken` từ frontend.
5. Gửi mail cuối ngày qua `POST /api/report-journals/submit-mail`.
6. Theo dõi log qua `GET /api/report-journals/email-logs`.

## 11. Build, test và kiểm tra nhanh

### Build local

```powershell
.\mvnw clean package -DskipTests
```

### Chạy test

```powershell
.\mvnw test
```

### Script có sẵn

- [test-build.cmd](D:\CheckSV\InternFlow\test-build.cmd)
- [test-build.sh](D:\CheckSV\InternFlow\test-build.sh)

Hiện tại source test trong `src/test` còn rất mỏng, chủ yếu mới có test khởi động context.

## 12. Database strategy hiện tại

Project hiện đang kết hợp 2 cách:

- Hibernate `ddl-auto=update` để tự cập nhật schema.
- Script SQL thủ công trong `db/migrations/`.

Điều này giúp phát triển nhanh ở giai đoạn MVP, nhưng có rủi ro:

- Khó kiểm soát khác biệt schema giữa các môi trường.
- Dễ lệch schema nếu quên chạy SQL thủ công.
- Khó rollback.

Khuyến nghị:

- Chuyển dần sang Flyway hoặc Liquibase.
- Giữ `ddl-auto=validate` ở production về sau.

## 13. Deploy

Project đã chuẩn bị sẵn cho Render:

- [render.yaml](D:\CheckSV\InternFlow\render.yaml)
- [Dockerfile](D:\CheckSV\InternFlow\Dockerfile)
- [QUICK_START.md](D:\CheckSV\InternFlow\QUICK_START.md)
- [RENDER_DEPLOYMENT.md](D:\CheckSV\InternFlow\RENDER_DEPLOYMENT.md)
- [DEPLOYMENT_CHECKLIST.md](D:\CheckSV\InternFlow\DEPLOYMENT_CHECKLIST.md)
- [RENDER_ENV_VARIABLES.md](D:\CheckSV\InternFlow\RENDER_ENV_VARIABLES.md)

### Điểm đặc biệt khi deploy Render free tier

- Service có thể sleep sau thời gian không có traffic.
- `KeepAliveScheduler` được thêm để tự ping giữ service tỉnh.
- Cần set `APP_BASE_URL` đúng URL production.

## 14. Tài liệu nội bộ đã có trong repo

Ngoài README này, repo đã có nhiều tài liệu chi tiết theo từng chủ đề:

- [DB_ERD.md](D:\CheckSV\InternFlow\DB_ERD.md): sơ đồ dữ liệu.
- [ADMIN_ROLE_MANAGEMENT_API.md](D:\CheckSV\InternFlow\ADMIN_ROLE_MANAGEMENT_API.md): API quản lý role.
- [TEAM_MEMBER_DETAIL_API.md](D:\CheckSV\InternFlow\TEAM_MEMBER_DETAIL_API.md): chi tiết thành viên.
- [TEAM_LEADER_FULL_DETAIL_API.md](D:\CheckSV\InternFlow\TEAM_LEADER_FULL_DETAIL_API.md): chi tiết đầy đủ cho leader.
- [MAIL_SYSTEM_REDESIGN.md](D:\CheckSV\InternFlow\MAIL_SYSTEM_REDESIGN.md): thiết kế mail.
- [CORS_SETUP.md](D:\CheckSV\InternFlow\CORS_SETUP.md): cấu hình CORS.

## 15. Các rủi ro và điểm nên cải tiến

Đây là các điểm mình thấy trực tiếp từ source hiện tại:

- Security đang mở toàn bộ API.
- Seed dữ liệu chứa email cụ thể của môi trường hiện tại.
- Chưa có migration framework chuẩn.
- Test coverage còn ít.
- Một số logic nghiệp vụ đang phụ thuộc khá nhiều vào frontend truyền đúng `userId`.
- Gửi mail phụ thuộc access token Google do frontend cấp, nên cần xử lý UX refresh token/quyền truy cập kỹ ở frontend.

## 16. Gợi ý roadmap kỹ thuật

Nếu tiếp tục phát triển backend này, nên ưu tiên:

1. Hoàn thiện auth + authorization ở backend.
2. Chuẩn hóa migration database.
3. Viết integration test cho các luồng: auth, schedule, attendance, report mail.
4. Tách config seed khỏi dữ liệu thật.
5. Bổ sung audit log và observability production.

## 17. Lệnh thường dùng

```powershell
.\mvnw spring-boot:run
.\mvnw clean package
.\mvnw test
curl http://localhost:8080/api/health
```

## 18. Kết luận

`InternFlow` backend hiện là một Spring Boot backend theo hướng MVP nhưng đã có khá đầy đủ luồng nghiệp vụ thực tế cho quản lý thực tập: từ đăng nhập, đăng ký ca, điểm danh, nhật ký, tới gửi mail báo cáo cuối ngày. Điểm mạnh là nghiệp vụ đã tương đối rõ ràng trong service layer; điểm cần ưu tiên tiếp theo là bảo mật, migration và test.
