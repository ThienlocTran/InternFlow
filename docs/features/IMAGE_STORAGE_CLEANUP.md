# Image storage and cleanup

InternFlow stores attendance images in Cloudinary for MVP/short-term storage. Frontend upload compresses images before upload, and UI list/dashboard/checklist screens use thumbnails by default. Full images should load only when the user opens/clicks a preview.

## Stored metadata

`attendance_images` keeps audit metadata:

- `imageUrl`
- `thumbnailUrl`
- `publicId`
- `storageProvider`
- `fileSizeBytes`
- `mimeType`
- `width` / `height`
- `retentionUntil`
- `deletedAt`
- `deleteStatus`

Cleanup never deletes database rows. After a Cloudinary delete succeeds, the row is kept and marked with `deletedAt` and `deleteStatus=DELETED`.

## Retention policy

- Images for a work date get `retentionUntil` only after Gmail API send succeeds or the student manually confirms the mail was sent.
- Default retention after mail send/confirmation: `IMAGE_RETENTION_AFTER_MAIL_DAYS=30`.
- Cohort-ended images are eligible after `cohort.end_date + IMAGE_COHORT_END_RETENTION_DAYS`, default `30`.
- Draft cleanup policy is `IMAGE_DRAFT_RETENTION_DAYS=7`, but automatic draft cleanup needs a persisted draft-image table/log first.
- Images without `publicId` are never deleted automatically.

## Cleanup safety

Safe defaults:

```bash
IMAGE_CLEANUP_ENABLED=false
IMAGE_CLEANUP_DRY_RUN=true
IMAGE_RETENTION_AFTER_MAIL_DAYS=30
IMAGE_DRAFT_RETENTION_DAYS=7
IMAGE_COHORT_END_RETENTION_DAYS=30
IMAGE_CLEANUP_BATCH_SIZE=50
IMAGE_CLEANUP_INTERVAL_MS=86400000
```

Enable cleanup in two steps only:

1. Set `IMAGE_CLEANUP_ENABLED=true` and keep `IMAGE_CLEANUP_DRY_RUN=true`.
2. Review logs and eligible query output.
3. Only after review, set `IMAGE_CLEANUP_DRY_RUN=false` in a controlled environment.

Do not enable real deletion in production until dry-run output has been reviewed.

## Eligible query summary

An attendance image is eligible only when:

- `publicId` is present.
- `deletedAt` is null.
- `deleteStatus` is not `DELETED`.
- `retentionUntil <= now` or the user's cohort ended before the configured cutoff.
- The image is within the configured batch limit.
