-- Migration: Add notes for role management feature
-- Date: 2026-05-18
-- Description: Document the role management feature for admin

-- This migration is for documentation purposes only
-- No schema changes are required as the role field already exists in app_users table

-- Role Management Rules:
-- 1. Admin can change user roles between: INTERN, TEAM_LEADER, MANAGER
-- 2. Admin cannot change role of another ADMIN user
-- 3. Admin cannot set role to ADMIN through API (only via email whitelist)
-- 4. Valid role transitions:
--    - INTERN <-> TEAM_LEADER
--    - INTERN <-> MANAGER
--    - TEAM_LEADER <-> MANAGER

-- Example queries to check user roles:

-- Get all users with their roles
-- SELECT id, email, full_name, role, active, created_at, updated_at 
-- FROM app_users 
-- ORDER BY role, full_name;

-- Get all team leaders
-- SELECT id, email, full_name, student_code, role 
-- FROM app_users 
-- WHERE role = 'TEAM_LEADER' AND active = true;

-- Get all interns
-- SELECT id, email, full_name, student_code, role 
-- FROM app_users 
-- WHERE role = 'INTERN' AND active = true;

-- Check if a user is leading any teams
-- SELECT t.id, t.name, u.full_name as leader_name, u.role
-- FROM teams t
-- JOIN app_users u ON t.leader_id = u.id
-- WHERE u.id = 'USER_UUID_HERE';

-- Note: When demoting a TEAM_LEADER to INTERN, consider checking if they are leading any teams
-- and handle the team leadership transfer if necessary
