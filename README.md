# Sierra Timesheet Management System
Time Sheet Management System
JakartaEE Web Applications 2026


## Script to create Database and Tables
##  Create User
```sql
create user 'APP'@'localhost' identified by 'APP';
grant all privileges on . to 'APP'@'localhost';
quit
```

## Create Database
```sql
create database sierra default character set = utf8mb4;
quit
```

## Create table PERSON
```sql
CREATE TABLE Person (
id                  BIGINT NOT NULL AUTO_INCREMENT,
first_name          VARCHAR(100) NOT NULL,
last_name           VARCHAR(100) NOT NULL,
date_of_birth       DATE,
email_address       VARCHAR(100) NOT NULL UNIQUE,
consent             BOOLEAN NOT NULL DEFAULT FALSE,
preferred_language  VARCHAR(10) NOT NULL DEFAULT 'en',
university_staff    BOOLEAN NOT NULL DEFAULT FALSE,
PRIMARY KEY (id)
);
```

## Create table ROLE
```sql
CREATE TABLE Role (
id         BIGINT NOT NULL AUTO_INCREMENT,
role_type  VARCHAR(20) NOT NULL,
person_id  BIGINT NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (person_id) REFERENCES person(id) ON DELETE CASCADE
);
```

## Create table CONTRACT
```sql
CREATE TABLE Contract (
id                      BIGINT NOT NULL AUTO_INCREMENT,
employee_id             BIGINT NOT NULL,
supervisor_id           BIGINT NOT NULL,
name                    VARCHAR(100) NOT NULL,
status                  VARCHAR(20) NOT NULL DEFAULT 'PREPARED',
start_date              DATE NOT NULL,
end_date                DATE NOT NULL,
frequency               VARCHAR(10) NOT NULL,
hours_per_week          DOUBLE NOT NULL,
working_days_per_week   INT NOT NULL DEFAULT 5,
vacation_days_per_year  INT NOT NULL DEFAULT 20,
termination_date        DATE,
archive_duration INT NOT NULL DEFAULT 24,
PRIMARY KEY (id),
FOREIGN KEY (employee_id) REFERENCES person(id),
FOREIGN KEY (supervisor_id) REFERENCES person(id)
);
```

## Create table CONTRACT_ASSISTANT join
```sql
CREATE TABLE Assistant_Contract (
assistant_id  BIGINT NOT NULL,
contract_id   BIGINT NOT NULL,
PRIMARY KEY (assistant_id, contract_id),
FOREIGN KEY (contract_id) REFERENCES contract(id) ON DELETE CASCADE,
FOREIGN KEY (assistant_id) REFERENCES person(id)
);
```

## Create table CONTRACT_SECRETARY join
```sql
CREATE TABLE Secretary_Contract (
secretary_id  BIGINT NOT NULL,
contract_id   BIGINT NOT NULL,
PRIMARY KEY (secretary_id, contract_id),
FOREIGN KEY (contract_id) REFERENCES contract(id) ON DELETE CASCADE,
FOREIGN KEY (secretary_id) REFERENCES person(id)
);
```

## Create table TIMESHEET
```sql
CREATE TABLE Timesheet (
id                    BIGINT NOT NULL AUTO_INCREMENT,
status                VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
start_date            DATE NOT NULL,
end_date              DATE NOT NULL,
signed_by_employee    DATE,
signed_by_supervisor  DATE,
contract_id           BIGINT NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (contract_id) REFERENCES contract(id) ON DELETE CASCADE
);
```

## Create table TIMESHEET_ENTRY
```sql
CREATE TABLE Timesheet_Entry (
id            BIGINT NOT NULL AUTO_INCREMENT,
timesheet_id  BIGINT NOT NULL,
type          VARCHAR(20) NOT NULL,
description   VARCHAR(500),
entry_date    DATE NOT NULL,
start_time    TIME NOT NULL,
end_time      TIME NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (timesheet_id) REFERENCES timesheet(id) ON DELETE CASCADE
);
```
