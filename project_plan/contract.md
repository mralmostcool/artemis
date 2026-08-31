# Contract Module Specification

## 1. Purpose
The **Contract Module** acts as the business execution engine for seafarer deployment. It manages employment contracts (Articles of Agreement) signed between shipping companies and seafarers, verifying maritime training certifications, medical fitness records, and coordinating physical sign-on/sign-off status transitions.

---

## 2. Capabilities
- Draft employment contracts linking seafarers, shipping companies, vessel allocations, and course credentials.
- Enforce strict validation:
  - A contract cannot be activated unless the seafarer has a `COMPLETED` enrollment record for their pre-sea courses.
  - A contract cannot be activated (signed-on) unless the seafarer has an active `is_fit = true` and non-expired record in the medical fitness registry (cross-module check with `seafarer` module).
  - Enforce training stint dates: Contracts must specify a minimum duration of **100 days**. MTIs can request extensions to these limits.
- Lock course completion: Block MTIs from modifying or cancelling course completions if there is a contract that depends on it.
- Record planned/actual logistics, next-of-kin information, monthly wages, and standard regulatory employment agreements.
- Automatically trigger sea time compilation upon contract completion.

---

## 3. Database Schema

Resides in the `public` schema. Includes cross-module integrity validation checks.

```sql
-- 1. Contract Status ENUM
CREATE TYPE public.contract_status AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'TERMINATED');

-- 2. Contract Table
CREATE TABLE public.contract (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
    company_id UUID NOT NULL REFERENCES public.company(id),
    enrollment_id UUID NOT NULL REFERENCES public.enrollment(id),
    berth_seafarer_allocation_id UUID NOT NULL REFERENCES public.berth_seafarer_allocation(id),
    status public.contract_status NOT NULL DEFAULT 'DRAFT',

    -- Terms of Agreement
    wage_monthly_usd NUMERIC(10, 2) NOT NULL DEFAULT 1000.00,
    agreement_type VARCHAR(100) NOT NULL DEFAULT 'ITF Standard CBA', -- e.g. 'CBA', 'Individual'
    rpsl_no VARCHAR(100) NOT NULL, -- copied from company at drafting
    
    -- Next of Kin details
    next_of_kin_name VARCHAR(255) NOT NULL,
    next_of_kin_relation VARCHAR(100) NOT NULL,
    next_of_kin_phone VARCHAR(50) NOT NULL,

    -- Planned logistics (Stated at start of the MTI course registration)
    sign_on_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sign_on_port VARCHAR(128) NOT NULL,
    sign_on_country VARCHAR(128) NOT NULL,
    sign_off_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sign_off_port VARCHAR(128) NOT NULL,
    sign_off_country VARCHAR(128) NOT NULL,

    -- Actual logistics (Recorded dynamically on embarkation/disembarkation)
    actual_sign_on_date TIMESTAMP WITH TIME ZONE,
    actual_sign_on_port VARCHAR(128),
    actual_sign_on_country VARCHAR(128),
    actual_sign_off_date TIMESTAMP WITH TIME ZONE,
    actual_sign_off_port VARCHAR(128),
    actual_sign_off_country VARCHAR(128),

    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Date logic validations
    CONSTRAINT chk_planned_dates CHECK (sign_off_date > sign_on_date),
    CONSTRAINT chk_minimum_stint_100_days CHECK (sign_off_date >= sign_on_date + INTERVAL '100 days'),
    CONSTRAINT chk_actual_dates CHECK (
        actual_sign_off_date IS NULL OR 
        actual_sign_on_date IS NULL OR 
        actual_sign_off_date > actual_sign_on_date
    )
);

-- Indices
CREATE INDEX idx_contract_indos_master_id ON public.contract(indos_master_id);
CREATE INDEX idx_contract_company_id ON public.contract(company_id);
CREATE INDEX idx_contract_enrollment_id ON public.contract(enrollment_id);
CREATE INDEX idx_contract_berth_seafarer_allocation_id ON public.contract(berth_seafarer_allocation_id);

-- Check Constraint Trigger (A contract requires COMPLETED enrollment)
CREATE OR REPLACE FUNCTION check_enrollment_completed()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.enrollment
        WHERE id = NEW.enrollment_id AND status = 'COMPLETED'
    ) THEN
        RAISE EXCEPTION 'Contract requires a COMPLETED enrollment (enrollment_id: %)', NEW.enrollment_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_contract_requires_completed_enrollment
BEFORE INSERT OR UPDATE ON public.contract
FOR EACH ROW EXECUTE FUNCTION check_enrollment_completed();

-- Triggers for updated_at & audit log
CREATE TRIGGER trg_contract_update_at BEFORE UPDATE ON public.contract FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_contract_audit AFTER INSERT OR UPDATE OR DELETE ON public.contract FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();

-- Locking trigger on public.enrollment to lock status changes if referenced
CREATE OR REPLACE FUNCTION check_no_contract_depends_on_enrollment()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'COMPLETED' AND NEW.status != 'COMPLETED' THEN
        IF EXISTS (
            SELECT 1 FROM public.contract
            WHERE enrollment_id = OLD.id
        ) THEN
            RAISE EXCEPTION 'Cannot modify enrollment: an active contract depends on this completed course (enrollment_id: %)', OLD.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enrollment_locked_by_contract
BEFORE UPDATE ON public.enrollment
FOR EACH ROW EXECUTE FUNCTION check_no_contract_depends_on_enrollment();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/contracts` | `GET` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | View contracts list. Candidates see own only. |
| `/api/v1/contracts/{contractId}` | `GET` | Authenticated | View specific contract details. Candidates can only view contracts assigned to them. |
| `/api/v1/contracts` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Draft a new employment contract (minimum 100 days stint duration). |
| `/api/v1/contracts/{contractId}/extend` | `POST` | `ROLE_INSTITUTE_ADMIN` | Request an extension of contract duration (stint extension). |
| `/api/v1/contracts/{contractId}` | `PUT` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Update contract terms (Allowed only if status is `DRAFT`). |
| `/api/v1/contracts/{contractId}` | `DELETE` | `ROLE_COMPANY_ADMIN` | Delete draft contract. |
| `/api/v1/contracts/{contractId}/sign-on` | `PUT` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Execute Actual Sign-on (Set state to `ACTIVE`). Checks medical fitness registry first. |
| `/api/v1/contracts/{contractId}/sign-off` | `PUT` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Execute Actual Sign-off (Set state to `COMPLETED` or `TERMINATED`). |

---

## 5. Endpoints & Sub-routes

### 5.1. Create Draft Contract
- **Path**: `/api/v1/contracts`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "indosMasterId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "companyId": "c1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "enrollmentId": "e1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "berthSeafarerAllocationId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "wageMonthlyUsd": 3500.00,
    "agreementType": "ITF Standard CBA",
    "nextOfKinName": "Jane Doe",
    "nextOfKinRelation": "Spouse",
    "nextOfKinPhone": "+919876543220",
    "signOnDate": "2026-09-01T08:00:00Z",
    "signOnPort": "Mumbai",
    "signOnCountry": "India",
    "signOffDate": "2026-12-10T08:00:00Z", -- Exactly 100 days
    "signOffPort": "Rotterdam",
    "signOffCountry": "Netherlands",
    "remarks": "Standard 6-month contract"
  }
  ```
- **Response `201 Created`**: Returns created contract details with status `DRAFT`.
- **Validation Failures**: Throws `400 Bad Request` if planned stint duration is less than 100 days.

### 5.2. Extend Stint Duration (MTI Request)
- **Path**: `/api/v1/contracts/{contractId}/extend`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "extendedSignOffDate": "2027-01-10T08:00:00Z",
    "extensionReason": "Academic curriculum validation requirements."
  }
  ```
- **Response `200 OK`**: Updated contract object with extended sign-off date.

### 5.3. Perform Actual Sign-On (Embarkation)
- **Path**: `/api/v1/contracts/{contractId}/sign-on`
- **Method**: `PUT`
- **Body**:
  ```json
  {
    "actualSignOnDate": "2026-09-01T12:00:00Z",
    "actualSignOnPort": "Mumbai",
    "actualSignOnCountry": "India"
  }
  ```
- **Response `200 OK`**: Returns updated contract details with status `ACTIVE`.
- **Validation Failures**: Throws `400 Bad Request` if seafarer does not have a valid, active medical fitness certificate.

### 5.4. Perform Actual Sign-Off (Disembarkation)
- **Path**: `/api/v1/contracts/{contractId}/sign-off`
- **Method**: `PUT`
- **Body**:
  ```json
  {
    "actualSignOffDate": "2027-03-02T10:00:00Z",
    "actualSignOffPort": "Rotterdam",
    "actualSignOffCountry": "Netherlands",
    "termination": false,
    "remarks": "Successfully completed contract term"
  }
  ```
- **Response `200 OK`**: Returns updated contract details with status `COMPLETED` (or `TERMINATED` if `termination` parameter is `true`).
- **Effect**: Computes and logs the final sea-service days duration into the seafarer profile metrics, and triggers concession credits updates for the allocating shipping company.
