-- Implicit casts for custom PostgreSQL enums to allow seamless Hibernate inserts/updates
CREATE CAST (varchar AS enrollment_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS contract_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS certificate_status) WITH INOUT AS IMPLICIT;
