-- Fix shift times to correct values
-- Ca 1: 08:00 - 11:30
-- Ca 2: 13:30 - 17:00
-- Ca 3: 17:00 - 19:40
-- Ca 4: 19:40 - 21:40

UPDATE shifts SET start_time = '08:00:00', end_time = '11:30:00' WHERE code = 'SHIFT_1';
UPDATE shifts SET start_time = '13:30:00', end_time = '17:00:00' WHERE code = 'SHIFT_2';
UPDATE shifts SET start_time = '17:00:00', end_time = '19:40:00' WHERE code = 'SHIFT_3';
UPDATE shifts SET start_time = '19:40:00', end_time = '21:40:00' WHERE code = 'SHIFT_4';
