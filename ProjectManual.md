# TSS | Time Sheet System

## Project Description

TSS is a time sheet management system for student assistant contracts at the
university. Supervisors and assistants create and start contracts, employees
report their working hours against the timesheets the system generates for
them, and supervisors sign off on those hours before secretaries archive
them. The system also calculates vacation allowance, sends reminder mails
for overdue action, and keeps statistics per contract.

- Git Repository Link: [https://gitlab.uni-koblenz.de/sierra1/sierra_timesheet_management_system](https://gitlab.uni-koblenz.de/sierra1/sierra_timesheet_management_system)

## Table of Contents

- [Project Description](#project-description)
- [Installation and Setup](#installation-and-setup)
- [Technologies Used](#technologies-used)
- [Domain Model](#domain-model)
- [Architecture](#architecture)
- [Features](#features)
- [Requirements Summary](#requirements-summary)
  - [Completed Requirements](#completed-requirements)
  - [Missed or Incomplete Requirements](#missed-or-incomplete-requirements)
- [Issues Encountered](#issues-encountered)
- [Decisions and Changes to Requirements](#decisions-and-changes-to-requirements)
- [Team Member Time Tracking](#team-member-time-tracking)
- [Contact](#contact)



## Installation and Setup

These steps take a fresh GlassFish 7 + MariaDB install to a running TSS.

1. Clone the repository:
  ```bash
   git clone https://gitlab.uni-koblenz.de/sierra1/sierra_timesheet_management_system.git
  ```
2. Open the project in NetBeans (the cloned folder already contains the
  NetBeans project files for `Sierra_2026-ejb`, `Sierra_2026-war`, and the
   enterprise application wrapping both).
3. Create the database and auth tables. With MariaDB running:
  ```bash
   mysql -u root -p < database/001-sierra-auth-schema.sql
  ```
   This creates the `sierra_tss` database and the two login tables
   (`SIERRA_AUTH_USER`, `SIERRA_AUTH_GROUP`). The application's own tables
   (`person`, `contract`, `timesheet`, `roles`, ...) are created
   automatically by JPA the first time the application deploys, so they do
   not need to be scripted.
4. In the GlassFish Admin Console, create a JDBC connection pool pointing at
  that database and a JDBC resource named `jdbc/sierra` for it. Full
   `asadmin` commands, including the exact properties GlassFish needs, are
   in `database/002-glassfish-setup.md`.
5. Create the `sierraRealm` authentication realm (JDBC realm, SHA-256/Hex
  digest, reading `SIERRA_AUTH_USER`/`SIERRA_AUTH_GROUP`). Command included
   in the same setup file.
6. Insert at least one login (see `database/002-glassfish-setup.md` for the
  exact statements) — there is no self-registration screen, accounts are
   created directly in the database.
7. Build the project (`ant clean dist`, or Clean and Build from NetBeans).
8. Deploy the resulting `.ear` to GlassFish and open
  `http://localhost:8080/Sierra_2026-war/`.

If a redeploy fails with "Expected to find an expanded directory for
submodule", undeploy, stop the domain, delete the application's folder under
`domains/domain1/applications/`, restart the domain, and deploy again — see
Issues Encountered below.

## Technologies Used

- Java, Jakarta EE 10
- JavaServer Faces (JSF) with Facelets
- PrimeFaces (UI components)
- GlassFish 7 application server
- Apache Ant (NetBeans build)
- MariaDB, MariaDB Connector/J
- EclipseLink (JPA provider)



## Domain Model

Core entities: `Person` (with `Role`s: Employee, Supervisor, Assistant,
Secretary), `Contract` (owns a set of `Timesheet`s, an employee, a
supervisor, and optional assistants/secretaries), `Timesheet` (owns a set of
`TimesheetEntry`, tracks its own status and signature dates), and
`TimesheetEntry` (a single reported block of work, vacation, or sick leave).

## Architecture

TSS follows the layered architecture of the course (SA1). It is packaged as
one enterprise application (`Sierra_2026_TimeSheetManagementSystem.ear`) with
two modules (SA2). The application packages are under the root package
`sierra.tms` (SA6).

### Modules and layers

| Layer | Module | Package | Contents |
| --- | --- | --- | --- |
| Presentation | `Sierra_2026-war` | `web/*.xhtml`, `web/resources` | JSF Facelets pages (contracts, timesheets, statistics, dashboards, login) and reusable composite components with their CSS |
| Presentation logic | `Sierra_2026-war` | `sierra.tms.web` | CDI backing beans (`ContractBean`, `TimesheetBean`, `StatisticsBean`, `DashboardBean`, `PersonBean`), `FeatureAccessBean` for role-based UI features, and the JSF exception handling |
| UI language | `Sierra_2026-war` | `sierra.tms.i18n` | `LocaleBean` and `UiMessages`, with `messages.properties` / `messages_de.properties` |
| Business logic | `Sierra_2026-ejb` | `sierra.tms.services`, `sierra.tms.services.impl` | Stateless EJBs behind local interfaces: contracts, timesheets, persons, hours and vacation calculation, public holidays, reminders |
| Data access | `Sierra_2026-ejb` | `sierra.tms.dao` | One DAO per entity, using JPA (EclipseLink) through the `Sierra-tms-pu` persistence unit and `jdbc/sierra` |
| Domain model | `Sierra_2026-ejb` | `sierra.tms.entities` | JPA entities `PersonEntity`, `RoleEntity`, `ContractEntity`, `TimesheetEntity`, `TimesheetEntryEntity`, `TimesheetSignatureEventEntity` |
| Data transfer | `Sierra_2026-ejb` | `sierra.tms.dto` | DTOs passed between the EJB and web modules, so entities never reach the pages |
| Support | `Sierra_2026-ejb` | `sierra.tms.utils`, `sierra.tms.utils.enums`, `sierra.tms.i18n`, `sierra.tms.exceptions` | `ConfigService` (reads `sierra.properties`), `ArchivingService`, enums, reminder texts in English and German, `TerminationWarning` |

The web module only talks to the EJB module through the service interfaces
and DTOs; pages never access DAOs or entities directly.

### Subsystems

- **Security.** Login uses a GlassFish JDBC realm (`sierraRealm`) on the
`SIERRA_AUTH_USER` / `SIERRA_AUTH_GROUP` tables. Every service method is
protected with `@RolesAllowed`, and the services additionally check that the
caller is assigned to the contract. In the web module, `FeatureAccessBean`
maps each role to the UI features it may see, so pages do not contain role
checks of their own.
- **Contracts and timesheets.** `ContractService` handles the contract
lifecycle and generates all timesheets when a contract starts.
`TimesheetService` handles entries, signatures, change requests and
archiving, and records every signature in `TimesheetSignatureEvent`.
- **Calculations.** `ContractHoursCalculationService` computes vacation hours,
hours due per timesheet and per contract, and remaining hours, using
`HolidayService` for the public holidays of the configured federal state.
- **Scheduled jobs.** EJB timers send the daily reminder mails
(`ReminderService`, through `mail/tssMailSession`) and delete expired
archived timesheets and contracts (`ArchivingService`).
- **Configuration.** Limits and defaults (maximum hours per week, default
working and vacation days, archive duration, federal state, holiday years,
time zone, mail sender) are read from `sierra.properties`, so they can be
changed without code changes.
- **Internationalization.** UI texts come from resource bundles in the web
module; reminder texts come from bundles in the EJB module and use each
recipient's preferred language.

### Database

MariaDB database `sierra`. `database/000-sierra-application-schema.sql`
creates the application tables and `database/001-sierra-auth-schema.sql` the
login tables; the numbered scripts after that add constraints and test data.

## Features

- Contract lifecycle: create, update while PREPARED, start, terminate,
archive.
- Automatic timesheet generation for a contract's whole duration once it is
started.
- Employees report work, vacation, and sick leave against IN_PROGRESS
timesheets, with overlap and vacation-allowance checks.
- Digital sign-off flow: employee signs, supervisor countersigns or returns
for changes, employee can revoke their own signature before it is
countersigned.
- Secretaries print and archive signed timesheets; archived timesheets and
their contracts are deleted automatically once the retention period
passes.
- Per-contract statistics: hours due, hours reported, balance, vacation used.
- Reminder mails for overdue timesheets and pending signatures.
- English and German interface, switchable at runtime.



## Requirements Summary

Completed Requirements

#### Contracts (CN)

- **CN1**: Assistants and supervisors can create, read, update and delete contracts; update and delete only work while the contract is PREPARED.
- **CN2**: Secretaries can print a contract as a cover page for the paper archive.
- **CN3**: Employees, assistants, supervisors and secretaries can view contract statistics (hours due, balance) on the Statistics page.
- **CN4**: Total hours due, vacation hours and remaining hours due are calculated, taking weekends and public holidays into account.
- **CN4a**: Vacation hours are calculated with the formula `vacationDaysPerYear * durationOfContract / 12 * hoursPerWeek / workingDaysPerWeek`.
- **CN4b**: Total hours due for a contract is the sum of the hours due of its individual timesheets.
- **CN4c**: Hours due for a timesheet is `(workingDaysInPeriod - publicHolidaysInPeriod) * hoursPerWeek / workingDaysPerWeek`, over the contract's weekly or monthly period.
- **CN4d**: Public holidays in Rhineland-Palatinate are determined for 2025–2030, including the movable ones (Easter, Ascension, Whit Monday).
- **CN4e**: Public holidays in Germany are determined for 2025–2030, with the federal state configurable (all 16 states supported).
- **CN5**: A contract is set to PREPARED as soon as it is created.
- **CN6**: Start date, end date, frequency, hours per week, vacation hours, working days per week and vacation days per year can only be changed while the contract is PREPARED.
- **CN6a**: Start date and end date must denote complete months (first day and last day of a month).
- **CN7**: Assistants and supervisors can start a contract; it moves from PREPARED to STARTED and every timesheet is created in IN_PROGRESS.
- **CN8**: Assistants and supervisors can terminate a started contract; it moves from STARTED to TERMINATED.
- **CN9**: A contract can only be aborted if its timesheets are in SIGNED_BY_SUPERVISOR, IN_PROGRESS or ARCHIVED.
- **CN10**: The termination date of a contract is recorded.
- **CN11**: Terminating a contract warns the user first if it has IN_PROGRESS timesheets with entries, so they can decide not to terminate yet.
- **CN12**: A contract is set to ARCHIVED as soon as all of its timesheets are ARCHIVED.



#### Time Sheets (TS)

- **TS1**: All timesheets for a contract are created, based on the timesheet frequency and the contract's start and end dates, as soon as the contract enters STARTED.
- **TS2**: Timesheet entries can only be added, changed or removed while the timesheet is IN_PROGRESS and its contract is STARTED.
- **TS3**: Total hours reported as VACATION cannot exceed the total vacation hours from CN4a.
- **TS4**: Timesheets in IN_PROGRESS are deleted as soon as the contract status changes to TERMINATED.
- **TS5**: Timesheets in SIGNED_BY_EMPLOYEE are never deleted.
- **TS6**: Timesheets in SIGNED_BY_SUPERVISOR are never deleted.
- **TS7**: Employees, assistants, supervisors and secretaries can view timesheets.
- **TS8**: Secretaries can print timesheets.
- **TS9**: Employees can manage their own timesheet entries; changes are only allowed while the timesheet is IN_PROGRESS.



#### Signatures (SG)

- **SG1**: Employees can sign a timesheet; it moves from IN_PROGRESS to SIGNED_BY_EMPLOYEE.
- **SG1a**: Employees can revoke their signature; the timesheet moves back from SIGNED_BY_EMPLOYEE to IN_PROGRESS.
- **SG2**: Supervisors can sign a timesheet that is SIGNED_BY_EMPLOYEE; it moves to SIGNED_BY_SUPERVISOR.
- **SG3**: Assistants and supervisors can request changes to a SIGNED_BY_EMPLOYEE timesheet; it moves back to IN_PROGRESS.



#### Reminders (RE)

- **RE1**: A reminder mail is sent to the employee on the last day of a timesheet if it is still IN_PROGRESS.
- **RE2**: A reminder mail is sent to the supervisor and the assistants when a timesheet is SIGNED_BY_EMPLOYEE.
- **RE3**: A reminder mail is sent to the secretaries when a timesheet is SIGNED_BY_SUPERVISOR.
- **RE4**: Reminders repeat every day for as long as the condition holds.
- **RE5**: All reminders due to one person on a given day are collected so they receive at most one e-mail per day.



#### Archiving (AR)

- **AR1**: Secretaries can archive timesheets that are SIGNED_BY_SUPERVISOR; the timesheet moves to ARCHIVED.
- **AR2**: Archived timesheets cannot be changed.
- **AR3**: Timesheets are deleted a fixed period after the supervisor's signature; a contract is deleted once all its timesheets are deleted.
- **AR4**: Archive duration is variable and stored per contract, defaulting to 24 months (2 years).



#### Access Control (AC)

- **AC1**: Users are authenticated before any data is accessible.
- **AC3**: Users can only view, change or delete data according to their staff status and their role on the specific contract — every service method is guarded by `@RolesAllowed`, and contract/timesheet operations additionally check that the caller is the employee, supervisor, assistant or secretary assigned to that contract.



#### User Interface (UI)

- **UI1**: The most frequent task for employees (report work) is accessible immediately after login.
- **UI4**: The PrimeFaces component library is used, including on the Statistics page.



#### Internationalization (IN)

- **IN1**: The user interface language can be switched.
- **IN2**: Users can choose their preferred language.
- **IN3**: At least two user interface languages are supported.
- **IN4**: English is supported as a user interface language.
- **IN5**: German is supported as a user interface language.
- **IN6**: Reminders are sent to users in their preferred language.



#### Software Architecture (SA)

- **SA1**: The TSS is implemented according to the layered architecture.
- **SA2**: The TSS contains at least two modules, a web module and an EJB module.
- **SA3**: No third-party libraries beyond those presented in the lecture/lab are used. The only additions to Jakarta EE are PrimeFaces (explicitly allowed) and the MariaDB Connector/J JDBC driver from the course setup, so no extra consent was needed.
- **SA4**: MariaDB is used as the database server.
- **SA5**: Architectural decisions (modules, layers, package structure and subsystems) are documented in the [Architecture](#architecture) section.
- **SA6**: All global names (database, JNDI names, security realm, root Java package) are prefixed with the team name (`sierra`).



Missed Requirements

- **SG4**: Digital signing of timesheets is not implemented. Signing records who signed and when (an audit entry tied to the logged-in session), but it does not create a cryptographic signature. A real digital signature needs keys or certificates for every employee and supervisor, and the university provides no such infrastructure for this project. As a SHOULD requirement, it was left out in favour of the MIN requirements.
- **AC2**: The TSS cannot determine on its own whether a person is a university staff member. Staff status is a `university_staff` flag that is set manually when the account is created, and the access checks rely on that flag. Determining it automatically would need a connection to the university's identity or HR directory (for example LDAP), which was not available to the team. Instead, the administrator creates the user accounts, keeps track of former employees, and deletes their contracts when required.
- **UI2**: Mobile devices are only partly supported. The pages set a viewport and have responsive CSS rules, but no dedicated mobile layout was designed and nothing was tested on phones or tablets. As a SHOULD requirement, it was deprioritised behind the MIN requirements. The application views work on mobile screens, but the login page is not adapted to mobile view.
- **UI3**: Cross-browser support (FireFox, Safari, Chrome) was not systematically tested; the team only tested in Chrome.



## Issues Encountered

- **Package split across two conventions.** Early contributions used
`com.jee.*` package names while later ones used `sierra.tms.*` for the
same classes (DAOs, DTOs, entities). Because several classes kept the old
package while living in the new source folders, `main` stopped compiling
more than once after a merge. Fixed by standardizing everything on
`sierra.tms.*` and re-pointing the handful of leftover imports.
- `request.isUserInRole(...)` **unreliable in JSF pages.** Several pages
used this EL call to show or hide role-specific buttons, but it
consistently evaluated to `false` even for a correctly authenticated user
with the right group, while the equivalent `@RolesAllowed` check on the
EJB side worked correctly. Worked around by resolving the current
person's roles through `PersonService.getCurrentPerson()` in the backing
bean instead of relying on the container check in the page.
- **Stale GlassFish deployment state.** Redeploying after a rebuild
intermittently failed with "Expected to find an expanded directory for
submodule ... .war", and once produced a
`ClassCastException: PersonEntity cannot be cast to PersonEntity` (two
copies of the same class loaded by two class loaders). Both were resolved
by fully undeploying, stopping the domain, deleting the leftover
application folder under `domains/domain1/applications/`, and starting a
clean deploy rather than an incremental one.
- **Floating point comparison on the vacation-hours cap.** The allowed
vacation hours formula divides by 12 and by the working days per week, so
the result is rarely an exact value (e.g. `48.999999999...` instead of
`49`). Comparing booked hours directly against that value rejected a
booking that matched the intended allowance exactly. Not yet fixed; a
small tolerance is planned for this comparison.
- **A single-digit hour in a timesheet entry (e.g.** `9:00`**) was rejected.**
Java's default time parser only accepts a zero-padded hour (`09:00`),
so a perfectly normal way of typing the time threw a parsing error with
no useful message. Fixed by falling back to a second, more lenient parser
when the strict one rejects the input.



## Decisions and Changes to Requirements

- All project-global identifiers (database name, JNDI names, the security
realm name, the root Java package) were prefixed with `sierra` once it
became clear several teams' applications share the same GlassFish
instance, to avoid resource name collisions at deployment.
- The vacation-hours check was kept strict (no rounding tolerance) for the
current submission rather than patched under time pressure; see Issues
Encountered. This is a deliberate, tracked decision, not an oversight.
- Weekly timesheet periods run from the contract's start date rather than
being re-aligned to calendar weeks (Monday–Sunday). Re-aligning them was
considered but not applied, since contracts are expected to start on the
first of a month in the common case and the change was not fully agreed
on by the team before the deadline.
- A dedicated Statistics page was added instead of folding contract
statistics into the existing Contracts page, so that the two pages could
be worked on independently without one team's change blocking another's.
- A person has exactly one role. The requirements describe roles per person
without saying whether several are allowed; the team chose one role per
person, enforced by a unique constraint on the `Roles` table and by
`assignRole()` replacing the current role rather than adding to it. This
keeps authorization decisions unambiguous.
- When an assistant or a secretary creates a contract, they are added to
that contract as an assistant or secretary by default. The requirements do
not say who is assigned to a new contract; the team chose to add the creator
automatically so that they can see and work with the contract they just
created without assigning themselves separately.
- An employee can have more than one contract, as the requirements allow,
but the hours per week of all their active contracts (PREPARED or STARTED)
together may not exceed 20. Creating a contract that would go over this
limit is rejected. The requirements do not set a limit; the team added it to
reflect the usual 20-hour weekly limit for student assistants. The value is
configurable in `sierra.properties` (`tss.contract.max-hours-per-week`).
Terminated and archived contracts do not count towards the limit.



## Team Member Time Tracking

Tracked via GitLab issues and merge requests on this project. Per-person
totals below are to be filled in from the issue tracker before final
submission.


| Team Member           | Main Areas                                                | Hours Spent |
| --------------------- | --------------------------------------------------------- | ----------- |
| Prajna Shetty         | UI design, Frontend Development, Backend Development      | 160         |
| Pranav Suresh Panhale | Frontend Development, Backend Development, Project Manual | 160         |
| Pranav Sudhir         | Frontend Development, Backend Development                 | 160         |
| Manas Gupta           | Frontend Development, Backend Development                 | 160         |




## Contact

For any further questions or support, please contact the project team at 

- [prajnashetty73@uni-koblenz.de](mailto:prajnashetty73@uni-koblenz.de)
- [pranavpanhale@uni-koblenz.de](mailto:pranavpanhale@uni-koblenz.de)
- [psudhir@uni-koblenz.de](mailto:psudhir@uni-koblenz.de)
- [manas_gupta@uni-koblenz.de](mailto:manas_gupta@uni-koblenz.de)

