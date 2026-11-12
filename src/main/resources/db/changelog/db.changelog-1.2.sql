--liquibase formatted sql

--changeset atlasrh:4-case-insensitive-unique-fields
CREATE UNIQUE INDEX uq_departments_name_lower ON departments (lower(name));
CREATE UNIQUE INDEX uq_employees_employee_number_lower ON employees (lower(employee_number));
CREATE UNIQUE INDEX uq_employees_email_lower ON employees (lower(email));

--rollback DROP INDEX uq_employees_email_lower; DROP INDEX uq_employees_employee_number_lower; DROP INDEX uq_departments_name_lower;
