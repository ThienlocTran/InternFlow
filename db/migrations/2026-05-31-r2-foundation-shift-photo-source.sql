-- R2-S1-DB-01/R2-S1-SHIFT-01: current schema rollup
-- Safe for an existing PostgreSQL database with sample data.
-- Run on local/staging with psql after backup/snapshot:
--   \i db/migrations/2026-05-31-r2-foundation-shift-photo-source.sql
-- Idempotent: safe to rerun; keeps existing rows.
-- Fresh DB baseline: db/init_production.sql

create extension if not exists pgcrypto;
-- Remove legacy MANAGER role. Current supported roles: INTERN, TEAM_LEADER, ADMIN.
update app_users
set role = 'TEAM_LEADER', updated_at = now()
where role = 'MANAGER';

delete from role_policies
where role = 'MANAGER';

delete from photo_requirements
where role = 'MANAGER';

alter table if exists app_users
    drop constraint if exists ck_app_users_role;

alter table if exists app_users
    add constraint ck_app_users_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN'));

alter table if exists role_policies
    drop constraint if exists ck_role_policies_role;

alter table if exists role_policies
    add constraint ck_role_policies_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN'));

alter table if exists photo_requirements
    drop constraint if exists ck_photo_requirements_role;

alter table if exists photo_requirements
    add constraint ck_photo_requirements_role check (role in ('INTERN', 'TEAM_LEADER', 'ADMIN'));

alter table if exists role_policies
    add column if not exists night_shift_bonus_threshold integer,
    add column if not exists night_shift_bonus_amount integer,
    add column if not exists leadership_bonus_threshold integer,
    add column if not exists leadership_bonus_amount integer;

update role_policies
set
    night_shift_bonus_threshold = case when role = 'INTERN' then 6 else 0 end,
    night_shift_bonus_amount = case when role = 'INTERN' then 1 else 0 end,
    leadership_bonus_threshold = case when role = 'TEAM_LEADER' then 6 else 0 end,
    leadership_bonus_amount = case when role = 'TEAM_LEADER' then 1 else 0 end
where night_shift_bonus_threshold is null
   or night_shift_bonus_amount is null
   or leadership_bonus_threshold is null
   or leadership_bonus_amount is null;

alter table if exists role_policies
    alter column night_shift_bonus_threshold set default 0,
    alter column night_shift_bonus_threshold set not null,
    alter column night_shift_bonus_amount set default 0,
    alter column night_shift_bonus_amount set not null,
    alter column leadership_bonus_threshold set default 0,
    alter column leadership_bonus_threshold set not null,
    alter column leadership_bonus_amount set default 0,
    alter column leadership_bonus_amount set not null;

alter table if exists attendances
    alter column checkin_group_image_url drop not null,
    alter column checkout_group_image_url drop not null;

create table if not exists email_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id) on delete cascade,
    subject varchar(500) not null,
    receivers text not null,
    cc_receivers text,
    work_date date not null,
    sent_at timestamp not null default now(),
    status varchar(50) not null,
    error_message text,
    attachment_count integer default 0,
    created_at timestamp not null default now()
);

create index if not exists idx_email_logs_user_id on email_logs (user_id);
create index if not exists idx_email_logs_work_date on email_logs (work_date desc);
create index if not exists idx_email_logs_sent_at on email_logs (sent_at desc);
create index if not exists idx_email_logs_status on email_logs (status);

alter table if exists shifts
    add column if not exists shift_order integer;

alter table if exists shifts
    add column if not exists display_group varchar(80),
    add column if not exists is_night_shift boolean default false;

update shifts
set shift_order = case code
    when 'SHIFT_1' then 1
    when 'SHIFT_2' then 2
    when 'SHIFT_3' then 3
    when 'SHIFT_4' then 4
    else coalesce(shift_order, 999)
end
where shift_order is null
   or code in ('SHIFT_1', 'SHIFT_2', 'SHIFT_3', 'SHIFT_4');

alter table if exists shifts
    alter column shift_order set default 0;

update shifts
set shift_order = 0
where shift_order is null;

update shifts set start_time = '08:00:00', end_time = '11:30:00' where code = 'SHIFT_1';
update shifts set start_time = '13:30:00', end_time = '17:00:00' where code = 'SHIFT_2';
update shifts set start_time = '17:00:00', end_time = '19:40:00' where code = 'SHIFT_3';
update shifts set start_time = '19:40:00', end_time = '21:40:00' where code = 'SHIFT_4';

update shifts
set display_group = case
    when code in ('SHIFT_3', 'SHIFT_4') then 'Buoi toi'
    when category = 'HOME_REPORT' then 'Bao cao tai nha'
    else 'Ban ngay'
end
where display_group is null;

update shifts
set is_night_shift = code in ('SHIFT_3', 'SHIFT_4')
where is_night_shift is null;

alter table if exists shifts
    alter column shift_order set not null,
    alter column is_night_shift set not null,
    alter column is_night_shift set default false;

create index if not exists idx_shifts_shift_order
    on shifts (shift_order);

alter table if exists attendance_images
    add column if not exists source_reference varchar(500);

alter table if exists report_entries
    add column if not exists source_references text;

alter table if exists attendance_images
    add column if not exists storage_provider varchar(50) default 'CLOUDINARY',
    add column if not exists public_id varchar(500),
    add column if not exists thumbnail_url varchar(500),
    add column if not exists file_size_bytes bigint,
    add column if not exists mime_type varchar(100),
    add column if not exists image_width integer,
    add column if not exists image_height integer,
    add column if not exists retention_until timestamp,
    add column if not exists deleted_at timestamp,
    add column if not exists delete_status varchar(30) default 'ACTIVE';

update attendance_images
set storage_provider = 'CLOUDINARY'
where storage_provider is null
  and image_url like '%cloudinary.com%';

update attendance_images
set thumbnail_url = image_url
where thumbnail_url is null
  and image_url is not null;

update attendance_images
set delete_status = 'ACTIVE'
where delete_status is null;

create index if not exists idx_attendance_images_public_id
    on attendance_images (public_id);

create index if not exists idx_attendance_images_retention
    on attendance_images (retention_until, deleted_at, delete_status);

do $$
declare
    photo_requirement_id_type text;
    photo_requirement_pk_name text;
    photo_requirement_max_id bigint;
begin
    if to_regclass('public.photo_requirements') is not null then
        select data_type
        into photo_requirement_id_type
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'photo_requirements'
          and column_name = 'id';

        if photo_requirement_id_type = 'uuid' then
            select conname
            into photo_requirement_pk_name
            from pg_constraint
            where conrelid = 'public.photo_requirements'::regclass
              and contype = 'p';

            if photo_requirement_pk_name is not null then
                execute format(
                    'alter table public.photo_requirements drop constraint %I',
                    photo_requirement_pk_name
                );
            end if;

            alter table public.photo_requirements
                add column if not exists id_bigint bigint;

            create sequence if not exists public.photo_requirements_id_seq;

            update public.photo_requirements
            set id_bigint = nextval('public.photo_requirements_id_seq')
            where id_bigint is null;

            select coalesce(max(id_bigint), 0)
            into photo_requirement_max_id
            from public.photo_requirements;

            if photo_requirement_max_id = 0 then
                perform setval('public.photo_requirements_id_seq', 1, false);
            else
                perform setval('public.photo_requirements_id_seq', photo_requirement_max_id, true);
            end if;

            alter table public.photo_requirements
                alter column id_bigint set default nextval('public.photo_requirements_id_seq'),
                alter column id_bigint set not null;

            alter sequence public.photo_requirements_id_seq
                owned by public.photo_requirements.id_bigint;

            alter table public.photo_requirements
                drop column id;

            alter table public.photo_requirements
                rename column id_bigint to id;

            alter sequence public.photo_requirements_id_seq
                owned by public.photo_requirements.id;

            alter table public.photo_requirements
                add constraint photo_requirements_pkey primary key (id);
        end if;
    end if;
end $$;

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

alter table if exists photo_requirements
    add column if not exists role varchar(30),
    add column if not exists shift_id uuid references shifts(id) on delete cascade,
    add column if not exists image_type varchar(30),
    add column if not exists phase varchar(30),
    add column if not exists required_count integer default 1,
    add column if not exists interval_minutes integer,
    add column if not exists active boolean default true,
    add column if not exists note varchar(500),
    add column if not exists created_at timestamp default now(),
    add column if not exists updated_at timestamp default now();

update photo_requirements
set required_count = 1
where required_count is null;

update photo_requirements
set active = true
where active is null;

update photo_requirements
set created_at = now()
where created_at is null;

update photo_requirements
set updated_at = now()
where updated_at is null;

alter table if exists photo_requirements
    alter column required_count set not null,
    alter column required_count set default 1,
    alter column active set not null,
    alter column active set default true,
    alter column created_at set not null,
    alter column created_at set default now(),
    alter column updated_at set not null,
    alter column updated_at set default now();

create unique index if not exists uk_photo_requirement_scope
    on photo_requirements (
        role,
        coalesce(shift_id, '00000000-0000-0000-0000-000000000000'::uuid),
        image_type,
        phase,
        coalesce(interval_minutes, -1)
    );

create index if not exists idx_photo_requirements_role_active
    on photo_requirements (role, active);

create index if not exists idx_photo_requirements_shift
    on photo_requirements (shift_id);

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

comment on column shifts.shift_order is 'Thu tu hien thi/xu ly ca, dung thay cho parse code SHIFT_n.';
comment on column shifts.display_group is 'Nhom hien thi cua ca, vi du Ban ngay/Buoi toi/Bao cao tai nha.';
comment on column shifts.is_night_shift is 'Danh dau ca toi de tinh rule/bonus, khong phu thuoc code ca.';
comment on table photo_requirements is 'Cau hinh yeu cau anh diem danh theo role/ca/loai anh/giai doan.';
comment on column photo_requirements.shift_id is 'Null = default cho moi ca; co gia tri = override cho ca cu the.';
comment on column photo_requirements.interval_minutes is 'Khoang lap anh giua ca; null cho moc checkin/checkout co dinh.';
comment on column attendance_images.source_reference is 'Nguon tham chieu/metadata cua anh, dung cho compliance audit.';
comment on column attendance_images.storage_provider is 'Storage backend for this image, currently CLOUDINARY for new uploads.';
comment on column attendance_images.public_id is 'Cloudinary public_id used for future delete API calls; legacy rows may be null.';
comment on column attendance_images.thumbnail_url is 'Lightweight image URL for dashboard/list rendering; falls back to image_url for legacy rows.';
comment on column attendance_images.file_size_bytes is 'Uploaded image size reported by Cloudinary or frontend/backend when available.';
comment on column attendance_images.mime_type is 'Uploaded image MIME type, for example image/webp or image/jpeg.';
comment on column attendance_images.image_width is 'Stored image width in pixels when available.';
comment on column attendance_images.image_height is 'Stored image height in pixels when available.';
comment on column attendance_images.retention_until is 'Future cleanup eligibility timestamp.';
comment on column attendance_images.deleted_at is 'Timestamp set after Cloudinary delete succeeds.';
comment on column attendance_images.delete_status is 'Lifecycle marker for cleanup, e.g. ACTIVE, DELETE_FAILED, DELETED.';
comment on column report_entries.source_references is 'Nguon tham khao co cau truc hoac snapshot link phuc vu compliance audit.';


update app_users
set role = 'TEAM_LEADER', updated_at = now()
where role = 'MANAGER';

delete from role_policies
where role = 'MANAGER';

delete from photo_requirements
where role = 'MANAGER';


update app_users
set role = 'ADMIN',
    active = true,
    updated_at = now()
where lower(email) = 'tuanprocc90@gmail.com';

update app_users
set role = 'TEAM_LEADER',
    active = true,
    updated_at = now()
where lower(email) = 'tuanprocc80@gmail.com';