alter table if exists role_policies
    add column if not exists night_shift_bonus_threshold integer;

alter table if exists role_policies
    add column if not exists night_shift_bonus_amount integer;

update role_policies
set
    night_shift_bonus_threshold = case
        when role = 'INTERN' then 6
        else 0
    end,
    night_shift_bonus_amount = case
        when role = 'INTERN' then 1
        else 0
    end
where night_shift_bonus_threshold is null
   or night_shift_bonus_amount is null;

alter table if exists role_policies
    alter column night_shift_bonus_threshold set default 0,
    alter column night_shift_bonus_threshold set not null;

alter table if exists role_policies
    alter column night_shift_bonus_amount set default 0,
    alter column night_shift_bonus_amount set not null;

update role_policies
set
    max_shifts_per_day = 2,
    target_shifts_per_week = 6,
    required_company_shifts = 60,
    required_home_shifts = 10,
    night_shift_bonus_threshold = 6,
    night_shift_bonus_amount = 1
where role = 'INTERN';

update role_policies
set
    max_shifts_per_day = 0,
    target_shifts_per_week = 0,
    required_company_shifts = 0,
    required_home_shifts = 0,
    night_shift_bonus_threshold = 0,
    night_shift_bonus_amount = 0
where role in ('TEAM_LEADER', 'MANAGER', 'ADMIN');
