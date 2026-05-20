
UPDATE app_users
SET role = 'ADMIN',
    updated_at = NOW()
WHERE LOWER(email) = 'zerosenpai3006@gmail.com';

-- Verify the update
SELECT id, email, full_name, role, active, updated_at
FROM app_users
WHERE LOWER(email) = 'zerosenpai3006@gmail.com';
