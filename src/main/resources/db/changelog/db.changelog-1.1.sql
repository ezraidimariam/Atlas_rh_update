--liquibase formatted sql

--changeset atlasrh:3-prevent-overlapping-contract-periods
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE contracts
    ADD CONSTRAINT contracts_employee_period_no_overlap
    EXCLUDE USING gist (
        employee_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    );

--rollback ALTER TABLE contracts DROP CONSTRAINT contracts_employee_period_no_overlap;
