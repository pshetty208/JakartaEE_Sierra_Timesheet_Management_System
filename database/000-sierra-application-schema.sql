CREATE DATABASE IF NOT EXISTS sierra DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sierra;

CREATE TABLE IF NOT EXISTS Person (
  id BIGINT NOT NULL AUTO_INCREMENT, first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL, date_of_birth DATE, email_address VARCHAR(255) NOT NULL,
  consent BOOLEAN NOT NULL DEFAULT FALSE, preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
  university_staff BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (id), UNIQUE (email_address)
);
CREATE TABLE IF NOT EXISTS Roles (
  id BIGINT NOT NULL AUTO_INCREMENT, role_type VARCHAR(20) NOT NULL, person_id BIGINT NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_roles_person (person_id),
  FOREIGN KEY (person_id) REFERENCES Person(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS Contract (
  id BIGINT NOT NULL AUTO_INCREMENT, employee_id BIGINT NOT NULL, supervisor_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PREPARED',
  start_date DATE NOT NULL, end_date DATE NOT NULL, frequency VARCHAR(10) NOT NULL,
  hours_per_week DOUBLE NOT NULL, working_days_per_week INT NOT NULL DEFAULT 5,
  vacation_days_per_year INT NOT NULL DEFAULT 20, termination_date DATE,
  archive_duration INT NOT NULL DEFAULT 24, PRIMARY KEY (id),
  FOREIGN KEY (employee_id) REFERENCES Person(id), FOREIGN KEY (supervisor_id) REFERENCES Person(id)
);
CREATE TABLE IF NOT EXISTS Assistant_Contract (
  assistant_id BIGINT NOT NULL, contract_id BIGINT NOT NULL, PRIMARY KEY (assistant_id, contract_id),
  FOREIGN KEY (assistant_id) REFERENCES Person(id),
  FOREIGN KEY (contract_id) REFERENCES Contract(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS Secretary_Contract (
  secretary_id BIGINT NOT NULL, contract_id BIGINT NOT NULL, PRIMARY KEY (secretary_id, contract_id),
  FOREIGN KEY (secretary_id) REFERENCES Person(id),
  FOREIGN KEY (contract_id) REFERENCES Contract(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS Timesheet (
  id BIGINT NOT NULL AUTO_INCREMENT, status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  start_date DATE NOT NULL, end_date DATE NOT NULL, signed_by_employee DATE,
  signed_by_supervisor DATE, contract_id BIGINT NOT NULL, PRIMARY KEY (id),
  FOREIGN KEY (contract_id) REFERENCES Contract(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS TIMESHEET_ENTRY (
  id BIGINT NOT NULL AUTO_INCREMENT, timesheet_id BIGINT NOT NULL, type VARCHAR(20) NOT NULL,
  description VARCHAR(500), entry_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,
  PRIMARY KEY (id), FOREIGN KEY (timesheet_id) REFERENCES Timesheet(id) ON DELETE CASCADE
);
