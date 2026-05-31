-- InternFlow production baseline schema
-- Purpose: single source for a fresh PostgreSQL database schema.
-- Do not run on an existing database with data. Use db/migrations/*.sql for upgrades.
-- Reviewed from JPA entities and current migrations as of 2026-05-31.

create extension if not exists pgcrypto;

create table if not exists internship_cohorts (
    id uuid primary key default gen_random_uuid(),
    code varchar(60) not null unique,
    name varchar(120) not null,
    start_date date not null,
    end_date date,
    active boolean not null default true,
    default_for_new_students boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table if not exists app_users (
    id uuid primary key default gen_random_uuid(),
    email varchar(150) not null unique,
    full_name varchar(120) not null,
    student_code varchar(50) unique,
    student_class varchar(80),
    school varchar(120),
    phone varchar(30),
    cohort_id uuid references internship_cohorts(id),
    role varchar(30) not null default 'INTERN',
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_app_users_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN'))
);

create table if not exists shifts (
    id uuid primary key default gen_random_uuid(),
    code varchar(30) not null unique,
    name varchar(80) not null,
    start_time time not null,
    end_time time not null,
    category varchar(30) not null default 'COMPANY',
    max_participants integer not null default 9,
    shift_order integer not null default 0,
    display_group varchar(80),
    is_night_shift boolean not null default false,
    active boolean not null default true,
    constraint ck_shifts_category check (category in ('COMPANY', 'HOME_REPORT')),
    constraint ck_shifts_max_participants check (max_participants >= 0),
    constraint ck_shifts_shift_order check (shift_order >= 0),
    constraint ck_shifts_time_range check (start_time < end_time)
);

create table if not exists role_policies (
    id uuid primary key default gen_random_uuid(),
    role varchar(30) not null unique,
    max_shifts_per_day integer not null,
    target_shifts_per_week integer not null,
    required_company_shifts integer not null,
    required_home_shifts integer not null,
    night_shift_bonus_threshold integer not null default 0,
    night_shift_bonus_amount integer not null default 0,
    leadership_bonus_threshold integer not null default 0,
    leadership_bonus_amount integer not null default 0,
    constraint ck_role_policies_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN')),
    constraint ck_role_policies_non_negative check (
        max_shifts_per_day >= 0
        and target_shifts_per_week >= 0
        and required_company_shifts >= 0
        and required_home_shifts >= 0
        and night_shift_bonus_threshold >= 0
        and night_shift_bonus_amount >= 0
        and leadership_bonus_threshold >= 0
        and leadership_bonus_amount >= 0
    )
);

create table if not exists teams (
    id uuid primary key default gen_random_uuid(),
    name varchar(100) not null unique,
    leader_id uuid not null references app_users(id),
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table if not exists team_members (
    id uuid primary key default gen_random_uuid(),
    team_id uuid not null references teams(id) on delete cascade,
    user_id uuid not null unique references app_users(id),
    joined_at timestamp not null default now()
);

create table if not exists schedule_registrations (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id),
    shift_id uuid not null references shifts(id),
    schedule_date date not null,
    status varchar(30) not null default 'REGISTERED',
    note varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_schedule_user_shift_date unique (user_id, shift_id, schedule_date),
    constraint ck_schedule_registrations_status check (status in ('REGISTERED', 'CANCELLED'))
);

create table if not exists attendances (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id),
    shift_id uuid not null references shifts(id),
    attendance_date date not null,
    status varchar(30) not null default 'PENDING',
    checkin_time timestamp,
    checkout_time timestamp,
    checkin_timemark_image_url varchar(500),
    checkin_group_image_url varchar(500),
    checkout_timemark_image_url varchar(500),
    checkout_group_image_url varchar(500),
    checkin_latitude double precision,
    checkin_longitude double precision,
    checkout_latitude double precision,
    checkout_longitude double precision,
    note varchar(500),
    report_page_count integer not null default 0,
    report_document_url varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_attendance_user_shift_date unique (user_id, shift_id, attendance_date),
    constraint ck_attendances_status check (status in ('PENDING', 'CHECKED_IN', 'CHECKED_OUT', 'ABSENT', 'REJECTED')),
    constraint ck_attendances_report_page_count check (report_page_count >= 0)
);

create table if not exists attendance_images (
    id uuid primary key default gen_random_uuid(),
    attendance_id uuid not null references attendances(id) on delete cascade,
    image_type varchar(30) not null,
    phase varchar(30) not null,
    expected_time time not null,
    image_url varchar(500) not null,
    storage_provider varchar(50) default 'CLOUDINARY',
    public_id varchar(500),
    thumbnail_url varchar(500),
    file_size_bytes bigint,
    mime_type varchar(100),
    image_width integer,
    image_height integer,
    source_reference varchar(500),
    display_order integer not null,
    note varchar(500),
    uploaded_at timestamp not null default now(),
    retention_until timestamp,
    deleted_at timestamp,
    delete_status varchar(30) default 'ACTIVE',
    constraint uk_attendance_image_slot unique (attendance_id, image_type, phase, expected_time),
    constraint ck_attendance_images_type check (image_type in ('PERSONAL_TIMEMARK', 'GROUP')),
    constraint ck_attendance_images_phase check (phase in ('CHECKIN', 'DURING_SHIFT', 'CHECKOUT')),
    constraint ck_attendance_images_file_size check (file_size_bytes is null or file_size_bytes >= 0),
    constraint ck_attendance_images_dimensions check (
        (image_width is null or image_width >= 0) and (image_height is null or image_height >= 0)
    )
);

create table if not exists report_documents (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique references app_users(id),
    title varchar(180) not null,
    total_pages integer not null default 0,
    completed_shift_count integer not null default 0,
    current_file_name varchar(220),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_report_documents_counts check (total_pages >= 0 and completed_shift_count >= 0)
);

create table if not exists report_entries (
    id uuid primary key default gen_random_uuid(),
    document_id uuid not null references report_documents(id) on delete cascade,
    work_date date not null,
    shift_codes varchar(120),
    shift_count integer not null,
    work_time_summary varchar(500),
    content text,
    reference_links text,
    source_references text,
    page_count integer not null,
    required_pages integer not null,
    status varchar(30) not null default 'DRAFT',
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_report_entry_document_date unique (document_id, work_date),
    constraint ck_report_entries_status check (status in ('DRAFT', 'READY_FOR_MAIL', 'NEEDS_MORE_PAGES')),
    constraint ck_report_entries_counts check (shift_count >= 0 and page_count >= 0 and required_pages >= 0)
);

create table if not exists report_revisions (
    id uuid primary key default gen_random_uuid(),
    entry_id uuid not null references report_entries(id) on delete cascade,
    old_content text,
    new_content text,
    diff_summary varchar(500),
    page_count_before integer not null,
    page_count_after integer not null,
    created_at timestamp not null default now(),
    constraint ck_report_revisions_counts check (page_count_before >= 0 and page_count_after >= 0)
);

create table if not exists email_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id),
    subject varchar(500) not null,
    receivers text not null,
    cc_receivers text,
    work_date date not null,
    sent_at timestamp not null,
    status varchar(50) not null,
    error_message text,
    attachment_count integer default 0,
    created_at timestamp not null default now(),
    constraint ck_email_logs_status check (status in ('PENDING', 'SENT', 'MANUAL_CONFIRMED', 'FAILED')),
    constraint ck_email_logs_attachment_count check (attachment_count is null or attachment_count >= 0)
);

create table if not exists photo_requirements (
    id bigint generated by default as identity primary key,
    role varchar(30) not null,
    shift_id uuid references shifts(id) on delete cascade,
    image_type varchar(30) not null,
    phase varchar(30) not null,
    required_count integer not null default 1,
    interval_minutes integer,
    active boolean not null default true,
    note varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_photo_requirements_required_count check (required_count >= 0),
    constraint ck_photo_requirements_interval_minutes check (interval_minutes is null or interval_minutes > 0),
    constraint ck_photo_requirements_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN')),
    constraint ck_photo_requirements_image_type check (image_type in ('PERSONAL_TIMEMARK', 'GROUP')),
    constraint ck_photo_requirements_phase check (phase in ('CHECKIN', 'DURING_SHIFT', 'CHECKOUT'))
);

create index if not exists idx_app_users_cohort on app_users(cohort_id);
create index if not exists idx_app_users_role_active on app_users(role, active);
create index if not exists idx_shifts_shift_order on shifts(shift_order);
create index if not exists idx_teams_leader on teams(leader_id);
create index if not exists idx_team_members_team on team_members(team_id);
create index if not exists idx_schedule_user_date on schedule_registrations(user_id, schedule_date);
create index if not exists idx_schedule_shift_date_status on schedule_registrations(shift_id, schedule_date, status);
create index if not exists idx_attendances_user_date on attendances(user_id, attendance_date);
create index if not exists idx_attendances_shift_date on attendances(shift_id, attendance_date);
create index if not exists idx_attendance_images_public_id on attendance_images(public_id);
create index if not exists idx_attendance_images_retention on attendance_images(retention_until, deleted_at, delete_status);
create index if not exists idx_report_entries_work_date on report_entries(work_date);
create index if not exists idx_report_revisions_entry on report_revisions(entry_id);
create index if not exists idx_email_logs_user_sent on email_logs(user_id, sent_at desc);
create index if not exists idx_email_logs_status_sent on email_logs(status, sent_at desc);
create unique index if not exists uk_photo_requirement_scope
    on photo_requirements (
        role,
        coalesce(shift_id, '00000000-0000-0000-0000-000000000000'::uuid),
        image_type,
        phase,
        coalesce(interval_minutes, -1)
    );
create index if not exists idx_photo_requirements_role_active on photo_requirements(role, active);
create index if not exists idx_photo_requirements_shift on photo_requirements(shift_id);

insert into shifts (code, name, start_time, end_time, category, max_participants, shift_order, display_group, is_night_shift, active)
values
    ('SHIFT_1', 'Ca 1', '08:00:00', '11:30:00', 'COMPANY', 9, 1, 'Ban ngay', false, true),
    ('SHIFT_2', 'Ca 2', '13:30:00', '17:00:00', 'COMPANY', 9, 2, 'Ban ngay', false, true),
    ('SHIFT_3', 'Ca 3', '17:00:00', '19:40:00', 'COMPANY', 9, 3, 'Buoi toi', true, true),
    ('SHIFT_4', 'Ca 4', '19:40:00', '21:40:00', 'COMPANY', 9, 4, 'Buoi toi', true, true)
on conflict (code) do nothing;

insert into role_policies (
    role,
    max_shifts_per_day,
    target_shifts_per_week,
    required_company_shifts,
    required_home_shifts,
    night_shift_bonus_threshold,
    night_shift_bonus_amount,
    leadership_bonus_threshold,
    leadership_bonus_amount
)
values
    ('INTERN', 2, 6, 60, 10, 6, 1, 0, 0),
    ('TEAM_LEADER', 3, 9, 60, 10, 6, 1, 6, 1),
    ('ADMIN', 0, 0, 0, 0, 0, 0, 0, 0)
on conflict (role) do nothing;

insert into photo_requirements (role, shift_id, image_type, phase, required_count, interval_minutes, active, note)
values
    ('INTERN', null, 'PERSONAL_TIMEMARK', 'CHECKIN', 1, null, true, 'Anh TimeMark vao ca'),
    ('INTERN', null, 'PERSONAL_TIMEMARK', 'DURING_SHIFT', 1, 30, true, 'Moi 30 phut trong ca'),
    ('INTERN', null, 'PERSONAL_TIMEMARK', 'CHECKOUT', 1, null, true, 'Anh TimeMark tan ca'),
    ('INTERN', null, 'GROUP', 'CHECKIN', 1, null, true, 'Anh nhom vao ca neu co nhom'),
    ('INTERN', null, 'GROUP', 'DURING_SHIFT', 1, 60, true, 'Moi 60 phut trong ca neu co nhom'),
    ('INTERN', null, 'GROUP', 'CHECKOUT', 1, null, true, 'Anh nhom tan ca neu co nhom'),
    ('TEAM_LEADER', null, 'PERSONAL_TIMEMARK', 'CHECKIN', 1, null, true, 'Anh TimeMark vao ca'),
    ('TEAM_LEADER', null, 'PERSONAL_TIMEMARK', 'DURING_SHIFT', 1, 30, true, 'Moi 30 phut trong ca'),
    ('TEAM_LEADER', null, 'PERSONAL_TIMEMARK', 'CHECKOUT', 1, null, true, 'Anh TimeMark tan ca'),
    ('TEAM_LEADER', null, 'GROUP', 'CHECKIN', 1, null, true, 'Anh nhom vao ca neu co nhom'),
    ('TEAM_LEADER', null, 'GROUP', 'DURING_SHIFT', 1, 60, true, 'Moi 60 phut trong ca neu co nhom'),
    ('TEAM_LEADER', null, 'GROUP', 'CHECKOUT', 1, null, true, 'Anh nhom tan ca neu co nhom')
on conflict do nothing;

comment on table internship_cohorts is 'Internship cohort/master intake table.';
comment on table app_users is 'Application users authenticated by Google OAuth.';
comment on table shifts is 'Configurable work shifts used for schedule and attendance.';
comment on table role_policies is 'Scheduling and completion policy by user role.';
comment on table teams is 'Team led by a team leader.';
comment on table team_members is 'One active team membership per user.';
comment on table schedule_registrations is 'User shift registrations by date.';
comment on table attendances is 'Actual attendance state per user/shift/date.';
comment on table attendance_images is 'Attendance photo slots and uploaded image metadata.';
comment on table report_documents is 'One report journal document per user.';
comment on table report_entries is 'Daily report journal entries.';
comment on table report_revisions is 'Report entry revision history.';
comment on table email_logs is 'Daily report email sending/audit log.';
comment on table photo_requirements is 'Configurable photo requirements by role/shift/type/phase.';
