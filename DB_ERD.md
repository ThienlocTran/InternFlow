# InternFlow Database ERD

Tài liệu này được dựng từ các entity JPA hiện có trong `src/main/java/com/java6/springboot/internflow/entity`, nên phản ánh cấu trúc database đang được backend sử dụng.

## ERD tổng quan

```mermaid
erDiagram
    APP_USERS {
        UUID id PK
        STRING email UK
        STRING full_name
        STRING student_code UK
        STRING student_class
        STRING school
        STRING phone
        UUID cohort_id FK
        STRING role
        BOOLEAN active
        INSTANT created_at
        INSTANT updated_at
    }

    INTERNSHIP_COHORTS {
        UUID id PK
        STRING code UK
        STRING name
        DATE start_date
        DATE end_date
        BOOLEAN active
        BOOLEAN default_for_new_students
        INSTANT created_at
        INSTANT updated_at
    }

    TEAMS {
        UUID id PK
        STRING name UK
        UUID leader_id FK
        BOOLEAN active
        INSTANT created_at
        INSTANT updated_at
    }

    TEAM_MEMBERS {
        UUID id PK
        UUID team_id FK
        UUID user_id FK_UK
        INSTANT joined_at
    }

    SHIFTS {
        UUID id PK
        STRING code UK
        STRING name
        TIME start_time
        TIME end_time
        STRING category
        INT max_participants
        BOOLEAN active
    }

    SCHEDULE_REGISTRATIONS {
        UUID id PK
        UUID user_id FK
        UUID shift_id FK
        DATE schedule_date
        STRING status
        STRING note
        INSTANT created_at
        INSTANT updated_at
    }

    ATTENDANCES {
        UUID id PK
        UUID user_id FK
        UUID shift_id FK
        DATE attendance_date
        STRING status
        INSTANT checkin_time
        INSTANT checkout_time
        STRING checkin_timemark_image_url
        STRING checkin_group_image_url
        STRING checkout_timemark_image_url
        STRING checkout_group_image_url
        DOUBLE checkin_latitude
        DOUBLE checkin_longitude
        DOUBLE checkout_latitude
        DOUBLE checkout_longitude
        STRING note
        INT report_page_count
        STRING report_document_url
        INSTANT created_at
        INSTANT updated_at
    }

    ATTENDANCE_IMAGES {
        UUID id PK
        UUID attendance_id FK
        STRING image_type
        STRING phase
        TIME expected_time
        STRING image_url
        INT display_order
        STRING note
        INSTANT uploaded_at
    }

    REPORT_DOCUMENTS {
        UUID id PK
        UUID user_id FK_UK
        STRING title
        INT total_pages
        INT completed_shift_count
        STRING current_file_name
        INSTANT created_at
        INSTANT updated_at
    }

    REPORT_ENTRIES {
        UUID id PK
        UUID document_id FK
        DATE work_date
        STRING shift_codes
        INT shift_count
        STRING work_time_summary
        TEXT content
        TEXT reference_links
        INT page_count
        INT required_pages
        STRING status
        INSTANT created_at
        INSTANT updated_at
    }

    REPORT_REVISIONS {
        UUID id PK
        UUID entry_id FK
        TEXT old_content
        TEXT new_content
        STRING diff_summary
        INT page_count_before
        INT page_count_after
        INSTANT created_at
    }

    EMAIL_LOGS {
        UUID id PK
        UUID user_id FK
        STRING subject
        TEXT receivers
        TEXT cc_receivers
        DATE work_date
        INSTANT sent_at
        STRING status
        TEXT error_message
        INT attachment_count
        INSTANT created_at
    }

    ROLE_POLICIES {
        UUID id PK
        STRING role UK
        INT max_shifts_per_day
        INT target_shifts_per_week
        INT required_company_shifts
        INT required_home_shifts
        INT night_shift_bonus_threshold
        INT night_shift_bonus_amount
        INT leadership_bonus_threshold
        INT leadership_bonus_amount
    }

    INTERNSHIP_COHORTS ||--o{ APP_USERS : "has"
    APP_USERS ||--o{ TEAMS : "leads"
    TEAMS ||--o{ TEAM_MEMBERS : "contains"
    APP_USERS ||--o| TEAM_MEMBERS : "belongs_to_one_team"

    APP_USERS ||--o{ SCHEDULE_REGISTRATIONS : "registers"
    SHIFTS ||--o{ SCHEDULE_REGISTRATIONS : "scheduled_in"

    APP_USERS ||--o{ ATTENDANCES : "checks_in_out"
    SHIFTS ||--o{ ATTENDANCES : "tracked_by"
    ATTENDANCES ||--o{ ATTENDANCE_IMAGES : "stores"

    APP_USERS ||--|| REPORT_DOCUMENTS : "owns"
    REPORT_DOCUMENTS ||--o{ REPORT_ENTRIES : "contains"
    REPORT_ENTRIES ||--o{ REPORT_REVISIONS : "keeps_history"

    APP_USERS ||--o{ EMAIL_LOGS : "sends"
```

## Nhóm quan hệ chính

1. `app_users` là bảng trung tâm.
2. `internship_cohorts` gắn với `app_users` qua `cohort_id`.
3. `teams` tham chiếu trưởng nhóm qua `leader_id -> app_users.id`.
4. `team_members` là bảng nối giữa `teams` và `app_users`, đồng thời ràng buộc `user_id` là duy nhất, nghĩa là một user chỉ thuộc tối đa một team.
5. `schedule_registrations` và `attendances` đều xoay quanh cặp `user + shift + date`.
6. `attendance_images` là bảng con của `attendances`.
7. `report_documents` là quan hệ 1-1 với `app_users`; mỗi user có tối đa một hồ sơ báo cáo.
8. `report_entries` là nhật ký theo ngày của một `report_document`, còn `report_revisions` là lịch sử chỉnh sửa của từng entry.
9. `email_logs` lưu lịch sử gửi mail theo user.
10. `role_policies` không có foreign key trực tiếp tới `app_users`, nhưng liên kết logic qua cột enum `role`.

## Ràng buộc đáng chú ý

- `team_members.user_id` là `UNIQUE`: một user không thể ở nhiều team cùng lúc.
- `report_documents.user_id` là `UNIQUE`: mỗi user chỉ có một document tổng.
- `schedule_registrations` unique theo `(user_id, shift_id, schedule_date)`.
- `attendances` unique theo `(user_id, shift_id, attendance_date)`.
- `report_entries` unique theo `(document_id, work_date)`.
- `attendance_images` unique theo `(attendance_id, image_type, phase, expected_time)`.
- `role_policies.role` là `UNIQUE`: mỗi role có tối đa một bộ policy.

## Quan hệ nghiệp vụ nhưng không có FK trực tiếp

- `role_policies.role` <-> `app_users.role`: map theo giá trị enum, không phải foreign key.
- `report_entries.work_date` có liên hệ nghiệp vụ với `attendances.attendance_date`, nhưng DB hiện không ràng buộc trực tiếp.
- `email_logs.work_date` cũng gắn theo ngày làm việc của user, nhưng không FK sang `report_entries` hoặc `attendances`.

## Gợi ý dùng cho team

- Nếu cần trình bày nhanh trong meeting, có thể tách ERD này thành 3 cụm:
  `Nhân sự` (`app_users`, `internship_cohorts`, `teams`, `team_members`, `role_policies`)
  `Lịch & chấm công` (`shifts`, `schedule_registrations`, `attendances`, `attendance_images`)
  `Báo cáo & email` (`report_documents`, `report_entries`, `report_revisions`, `email_logs`)
