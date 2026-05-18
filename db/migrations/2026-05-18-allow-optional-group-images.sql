alter table if exists attendances
    alter column checkin_group_image_url drop not null,
    alter column checkout_group_image_url drop not null;
