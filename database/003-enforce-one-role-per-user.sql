-- Apply after resolving any users that currently have more than one role.
-- These queries must return no rows before applying the constraints:
-- SELECT person_id FROM Roles GROUP BY person_id HAVING COUNT(*) > 1;
-- SELECT username FROM SIERRA_AUTH_GROUP GROUP BY username HAVING COUNT(*) > 1;

USE sierra;

ALTER TABLE Roles
  DROP INDEX person_id,
  ADD UNIQUE KEY uq_roles_person (person_id);

ALTER TABLE SIERRA_AUTH_GROUP
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (username);
