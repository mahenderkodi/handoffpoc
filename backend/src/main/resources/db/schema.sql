-- Employee Management POC — schema (Section 6 of IMPLEMENTATION_INSTRUCTIONS.md)
--
-- No migration tool (Flyway/Liquibase) is used, by explicit instruction. Spring Boot runs this
-- file automatically on startup (spring.sql.init.mode) and Hibernate only VALIDATES against it
-- (ddl-auto: validate) — this file is the single source of truth for structure.
--
-- Trade-off, stated explicitly: this file has no migration history and is NOT safe to re-run
-- against a database that already has diverging data (e.g. a column added by hand). That is
-- an accepted limitation of this POC, not an oversight.

CREATE TABLE IF NOT EXISTS employee (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    employee_code    VARCHAR(20)  NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    phone            VARCHAR(20)  NULL,
    department       VARCHAR(100) NOT NULL,
    job_title        VARCHAR(100) NOT NULL,
    date_of_joining  DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_employee_employee_code UNIQUE (employee_code),
    CONSTRAINT uq_employee_email UNIQUE (email),
    -- Deliberately a VARCHAR + CHECK, not MySQL's native ENUM (harder to change later, maps
    -- awkwardly to Hibernate) — Section 6.
    CONSTRAINT chk_employee_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- department / status filtering, and first/last-name search (Section 7.2). email and
-- employee_code already have an index via their UNIQUE constraints above.
CREATE INDEX idx_employee_department ON employee (department);
CREATE INDEX idx_employee_status ON employee (status);
CREATE INDEX idx_employee_last_name ON employee (last_name);
CREATE INDEX idx_employee_first_name ON employee (first_name);
