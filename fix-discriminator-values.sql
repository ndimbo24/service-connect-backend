-- Fix discriminator values in users table
-- The dtype column should contain: 'client', 'technician', or 'admin'
-- NOT 'User'

-- 1. Check current bad rows
SELECT id, phone, dtype FROM users WHERE dtype = 'User' OR dtype IS NULL;

-- 2. Fix rows with dtype='User' - determine actual type from linked tables
-- If user has a client record, it's a client
UPDATE users u
SET dtype = 'client'
WHERE dtype = 'User'
  AND EXISTS (SELECT 1 FROM clients c WHERE c.user_id = u.id);

-- If user has a technician record, it's a technician
UPDATE users u
SET dtype = 'technician'
WHERE dtype = 'User'
  AND EXISTS (SELECT 1 FROM technicians t WHERE t.user_id = u.id);

-- If user is the admin (check by role or phone), set as admin
UPDATE users u
SET dtype = 'admin'
WHERE dtype = 'User'
  AND (u.role = 'admin' OR u.phone = '1111111111');

-- 3. Verify fix
SELECT id, phone, dtype FROM users WHERE dtype NOT IN ('client', 'technician', 'admin');

-- 4. If any remain with dtype='User' and no matching child table, 
--    either delete them or set appropriate dtype based on your data
--    Example for admin seed:
UPDATE users SET dtype = 'admin' WHERE phone = '1111111111';
