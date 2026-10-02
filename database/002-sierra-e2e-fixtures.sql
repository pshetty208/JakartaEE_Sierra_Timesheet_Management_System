-- Safe, rerunnable local fixtures for the Sierra UI.
-- This version deliberately resolves people by email; it does not assume ids.

USE sierra;

START TRANSACTION;

INSERT INTO SIERRA_AUTH_USER (username, password_hash)
VALUES
  ('tester@sierra.de', SHA2('tester', 256)),
  ('supervisor@sierra.de', SHA2('supervisor', 256)),
  ('outsider@sierra.de', SHA2('outsider', 256))
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

INSERT IGNORE INTO SIERRA_AUTH_GROUP (username, group_name)
VALUES
  ('tester@sierra.de', 'EMPLOYEE'),
  ('supervisor@sierra.de', 'SUPERVISOR'),
  ('outsider@sierra.de', 'EMPLOYEE');

INSERT INTO Person (first_name, last_name, email_address, university_staff, preferred_language, consent)
VALUES
  ('Test', 'Employee', 'tester@sierra.de', FALSE, 'en', TRUE),
  ('Test', 'Supervisor', 'supervisor@sierra.de', TRUE, 'en', TRUE),
  ('Test', 'Outsider', 'outsider@sierra.de', FALSE, 'en', TRUE)
ON DUPLICATE KEY UPDATE
  first_name = VALUES(first_name),
  last_name = VALUES(last_name),
  university_staff = VALUES(university_staff),
  consent = VALUES(consent);

SET @employee_id = (SELECT id FROM Person WHERE email_address = 'tester@sierra.de');
SET @supervisor_id = (SELECT id FROM Person WHERE email_address = 'supervisor@sierra.de');

INSERT INTO Roles (role_type, person_id)
SELECT 'EMPLOYEE', @employee_id
WHERE NOT EXISTS (SELECT 1 FROM Roles WHERE role_type = 'EMPLOYEE' AND person_id = @employee_id);

INSERT INTO Roles (role_type, person_id)
SELECT 'SUPERVISOR', @supervisor_id
WHERE NOT EXISTS (SELECT 1 FROM Roles WHERE role_type = 'SUPERVISOR' AND person_id = @supervisor_id);

INSERT INTO Contract (employee_id, supervisor_id, name, status, start_date, end_date, frequency,
                      hours_per_week, working_days_per_week, vacation_days_per_year, archive_duration)
SELECT @employee_id, @supervisor_id, 'Prepared Contract (UI Test)', 'PREPARED',
       '2026-09-01', '2026-12-31', 'WEEKLY', 8, 5, 20, 24
WHERE NOT EXISTS (SELECT 1 FROM Contract WHERE name = 'Prepared Contract (UI Test)');

INSERT INTO Contract (employee_id, supervisor_id, name, status, start_date, end_date, frequency,
                      hours_per_week, working_days_per_week, vacation_days_per_year, archive_duration)
SELECT @employee_id, @supervisor_id, 'Started Contract - Clean Terminate (UI Test)', 'STARTED',
       '2026-07-01', '2026-12-31', 'WEEKLY', 10, 5, 20, 24
WHERE NOT EXISTS (SELECT 1 FROM Contract WHERE name = 'Started Contract - Clean Terminate (UI Test)');

INSERT INTO Contract (employee_id, supervisor_id, name, status, start_date, end_date, frequency,
                      hours_per_week, working_days_per_week, vacation_days_per_year, archive_duration)
SELECT @employee_id, @supervisor_id, 'Started Contract - Warning Terminate (UI Test)', 'STARTED',
       '2026-07-01', '2026-12-31', 'MONTHLY', 20, 5, 20, 24
WHERE NOT EXISTS (SELECT 1 FROM Contract WHERE name = 'Started Contract - Warning Terminate (UI Test)');

SET @warning_contract_id = (
  SELECT id FROM Contract WHERE name = 'Started Contract - Warning Terminate (UI Test)'
);

INSERT INTO Timesheet (contract_id, status, start_date, end_date)
SELECT @warning_contract_id, 'IN_PROGRESS', '2026-08-01', '2026-08-31'
WHERE NOT EXISTS (
  SELECT 1 FROM Timesheet
  WHERE contract_id = @warning_contract_id AND start_date = '2026-08-01' AND end_date = '2026-08-31'
);

SET @warning_timesheet_id = (
  SELECT id FROM Timesheet
  WHERE contract_id = @warning_contract_id AND start_date = '2026-08-01' AND end_date = '2026-08-31'
);

INSERT INTO TIMESHEET_ENTRY (timesheet_id, type, description, entry_date, start_time, end_time)
SELECT @warning_timesheet_id, 'WORK', 'Research work', '2026-08-04', '09:00:00', '13:00:00'
WHERE NOT EXISTS (
  SELECT 1 FROM TIMESHEET_ENTRY
  WHERE timesheet_id = @warning_timesheet_id AND description = 'Research work'
);

INSERT INTO TIMESHEET_ENTRY (timesheet_id, type, description, entry_date, start_time, end_time)
SELECT @warning_timesheet_id, 'WORK', 'Weekly meeting', '2026-08-05', '10:00:00', '11:00:00'
WHERE NOT EXISTS (
  SELECT 1 FROM TIMESHEET_ENTRY
  WHERE timesheet_id = @warning_timesheet_id AND description = 'Weekly meeting'
);

INSERT INTO Contract (employee_id, supervisor_id, name, status, start_date, end_date, frequency,
                      hours_per_week, working_days_per_week, vacation_days_per_year, archive_duration,
                      termination_date)
SELECT @employee_id, @supervisor_id, 'Terminated Contract (UI Test)', 'TERMINATED',
       '2026-01-01', '2026-06-30', 'WEEKLY', 12, 5, 20, 24, '2026-05-15'
WHERE NOT EXISTS (SELECT 1 FROM Contract WHERE name = 'Terminated Contract (UI Test)');

COMMIT;
