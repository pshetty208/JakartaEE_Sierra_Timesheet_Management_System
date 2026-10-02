-- Local test logins for the ASSISTANT, SECRETARY and ADMIN roles.
-- Run after 002-sierra-e2e-fixtures.sql. Safe to re-run.
--
--   assistant@sierra.de / assistant  (ASSISTANT, assigned to every UI-test contract)
--   secretary@sierra.de / secretary  (SECRETARY, assigned to every UI-test contract)
--   admin@sierra.de     / admin      (ADMIN)
--
-- All three are university staff: the application rejects the ASSISTANT,
-- SECRETARY and ADMIN roles for anyone who is not.

USE sierra;

START TRANSACTION;

INSERT INTO SIERRA_AUTH_USER (username, password_hash)
VALUES
  ('assistant@sierra.de', SHA2('assistant', 256)),
  ('secretary@sierra.de', SHA2('secretary', 256)),
  ('admin@sierra.de', SHA2('admin', 256))
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- One group per user (primary key is username).
INSERT INTO SIERRA_AUTH_GROUP (username, group_name)
VALUES
  ('assistant@sierra.de', 'ASSISTANT'),
  ('secretary@sierra.de', 'SECRETARY'),
  ('admin@sierra.de', 'ADMIN')
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name);

INSERT INTO Person (first_name, last_name, email_address, university_staff, preferred_language, consent)
VALUES
  ('Test', 'Assistant', 'assistant@sierra.de', TRUE, 'en', TRUE),
  ('Test', 'Secretary', 'secretary@sierra.de', TRUE, 'en', TRUE),
  ('Test', 'Admin', 'admin@sierra.de', TRUE, 'en', TRUE)
ON DUPLICATE KEY UPDATE
  university_staff = VALUES(university_staff),
  consent = VALUES(consent);

SET @assistant_id = (SELECT id FROM Person WHERE email_address = 'assistant@sierra.de');
SET @secretary_id = (SELECT id FROM Person WHERE email_address = 'secretary@sierra.de');
SET @admin_id = (SELECT id FROM Person WHERE email_address = 'admin@sierra.de');

-- One role per person (unique on person_id).
INSERT INTO Roles (role_type, person_id) VALUES ('ASSISTANT', @assistant_id)
ON DUPLICATE KEY UPDATE role_type = VALUES(role_type);
INSERT INTO Roles (role_type, person_id) VALUES ('SECRETARY', @secretary_id)
ON DUPLICATE KEY UPDATE role_type = VALUES(role_type);
INSERT INTO Roles (role_type, person_id) VALUES ('ADMIN', @admin_id)
ON DUPLICATE KEY UPDATE role_type = VALUES(role_type);

-- Assign the assistant and secretary to every UI-test contract, so they can see and act on them.
INSERT IGNORE INTO Assistant_Contract (assistant_id, contract_id)
SELECT @assistant_id, id FROM Contract WHERE name LIKE '%(UI Test)';

INSERT IGNORE INTO Secretary_Contract (secretary_id, contract_id)
SELECT @secretary_id, id FROM Contract WHERE name LIKE '%(UI Test)';

COMMIT;
