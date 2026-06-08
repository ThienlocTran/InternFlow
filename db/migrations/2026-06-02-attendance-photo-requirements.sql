-- R2-S1-PHOTO-01: per-attendance expected photo checklist slots.

create extension if not exists pgcrypto;

create table if not exists attendance_photo_requirements (
    id uuid primary key default gen_random_uuid(),
    attendance_id uuid not null references attendances(id) on delete cascade,
    image_type varchar(30) not null,
    phase varchar(30) not null,
    expected_time time not null,
    required boolean not null default true,
    status varchar(30) not null default 'PENDING',
    skip_reason varchar(500),
    attendance_image_id uuid references attendance_images(id) on delete set null,
    note varchar(500),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_attendance_photo_requirement_slot unique (attendance_id, image_type, phase, expected_time),
    constraint ck_attendance_photo_requirement_type check (image_type in ('PERSONAL_TIMEMARK', 'GROUP')),
    constraint ck_attendance_photo_requirement_phase check (phase in ('CHECKIN', 'DURING_SHIFT', 'CHECKOUT')),
    constraint ck_attendance_photo_requirement_status check (status in ('PENDING', 'SATISFIED', 'SKIPPED')),
    constraint ck_attendance_photo_requirement_skip check (status = 'SKIPPED' or skip_reason is null)
);

create index if not exists idx_attendance_photo_requirements_attendance
    on attendance_photo_requirements(attendance_id, expected_time);

create index if not exists idx_attendance_photo_requirements_status
    on attendance_photo_requirements(status);

create index if not exists idx_attendance_photo_requirements_image
    on attendance_photo_requirements(attendance_image_id);

comment on table attendance_photo_requirements is 'Expected photo checklist slots materialized per attendance.';
comment on column attendance_photo_requirements.required is 'False means optional slot; status still tracks uploaded/skipped state.';
comment on column attendance_photo_requirements.attendance_image_id is 'Uploaded image satisfying this expected slot, if any.';
