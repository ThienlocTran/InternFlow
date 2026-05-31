-- Phase 3: Cloudinary metadata for attendance_images.
-- Safe for existing rows: all metadata fields are nullable except defaults.
-- Existing image_url-only records remain valid; public_id can stay null.

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

comment on column attendance_images.storage_provider is 'Storage backend for this image, currently CLOUDINARY for new uploads.';
comment on column attendance_images.public_id is 'Cloudinary public_id used for future delete API calls; legacy rows may be null.';
comment on column attendance_images.thumbnail_url is 'Lightweight image URL for dashboard/list rendering; falls back to image_url for legacy rows.';
comment on column attendance_images.file_size_bytes is 'Uploaded image size reported by Cloudinary or frontend/backend when available.';
comment on column attendance_images.mime_type is 'Uploaded image MIME type, for example image/webp or image/jpeg.';
comment on column attendance_images.image_width is 'Stored image width in pixels when available.';
comment on column attendance_images.image_height is 'Stored image height in pixels when available.';
comment on column attendance_images.retention_until is 'Future cleanup eligibility timestamp; Phase 3 does not delete images.';
comment on column attendance_images.deleted_at is 'Timestamp set by future cleanup after Cloudinary delete succeeds.';
comment on column attendance_images.delete_status is 'Lifecycle marker for future cleanup, e.g. ACTIVE, DELETE_FAILED, DELETED.';
