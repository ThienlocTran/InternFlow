alter table role_policies
    add column if not exists leadership_bonus_threshold integer;

alter table role_policies
    add column if not exists leadership_bonus_amount integer;

update role_policies
set leadership_bonus_threshold = case
        when role = 'TEAM_LEADER' then 6
        else 0
    end,
    leadership_bonus_amount = case
        when role = 'TEAM_LEADER' then 1
        else 0
    end
where leadership_bonus_threshold is null
   or leadership_bonus_amount is null;

alter table role_policies
    alter column leadership_bonus_threshold set default 0,
    alter column leadership_bonus_threshold set not null;

alter table role_policies
    alter column leadership_bonus_amount set default 0,
    alter column leadership_bonus_amount set not null;
