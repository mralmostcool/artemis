# Vessel, Company & Berth Allocation Module Specification

## 1. Purpose
The **Vessel, Company & Berth Allocation Module** manages shipping companies, their vessels, ports/berths, and resources. It coordinates the logistics of scheduling vessel port stays, allocating training berths, tracking DG Shipping concessions, allocating seafarers to designated berths, and generating official vessel manifests.

---

## 2. Capabilities
- Manage shipping company profiles (`company`), including Recruitment and Placement Services License (RPSL) credentials.
- Register company vessels and associate them with dimensions, propulsion specifications, type classifications, and IMO registry codes (`vessel`).
- Request training berth allocations on vessels to qualify for DG Shipping Concession Credits.
- Manage physical harbor berths (`berth`), including length (LOA) and draft constraints.
- Allocate vessels to berths for a minimum of **1 year** (in increments of years) while preventing double-bookings.
- Allocate seafarers to berths while ensuring date overlap validations (`berth_seafarer_allocation`). Ensure no two candidates occupy the same seat at the same time.
- Track concession metrics ledger for shipping companies based on active seafarer training days.
- Provide timeline and availability scheduling endpoints to feed Gantt charts.
- Generate standard IMO Crew List manifests for port clearance.

---

## 3. Database Schema

Resides in the `public` schema. Incorporates GIST exclusion constraints for temporal safety.

```sql
-- 1. Company Table
CREATE TABLE public.company (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_no VARCHAR(64) UNIQUE, -- Business registry number
    rpsl_no VARCHAR(100) UNIQUE,        -- Recruitment & Placement Services License number (mandatory in India)
    rpsl_valid_until DATE,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Vessel Table
CREATE TABLE public.vessel (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    imo VARCHAR(10) NOT NULL UNIQUE, -- IMO registry number (e.g. IMO9123456)
    name VARCHAR(128) NOT NULL,
    flag VARCHAR(64) NOT NULL,
    vessel_type VARCHAR(100) NOT NULL DEFAULT 'Bulk Carrier', -- 'Tanker', 'Container', 'LNG', etc.
    call_sign VARCHAR(50) UNIQUE,
    grt NUMERIC(10, 2), -- Gross Register Tonnage
    nrt NUMERIC(10, 2), -- Net Register Tonnage
    engine_power_kw INTEGER,
    year_built INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    company_id UUID NOT NULL REFERENCES public.company(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Berth Table
CREATE TABLE public.berth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    berth_name VARCHAR(128) NOT NULL UNIQUE,
    max_draft_meters NUMERIC(5, 2) NOT NULL DEFAULT 10.00, -- Depth constraint
    max_loa_meters NUMERIC(6, 2) NOT NULL DEFAULT 200.00,  -- Length Overall constraint
    coordinate_x NUMERIC(10, 6),
    coordinate_y NUMERIC(10, 6),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Berth Allocation Table (Vessel stay scheduling - minimum 1 year, in increments of years)
CREATE TABLE public.berth_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    berth_id UUID NOT NULL REFERENCES public.berth(id),
    vessel_id UUID NOT NULL REFERENCES public.vessel(id),
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_berth_alloc_dates CHECK (end_date > start_date),
    CONSTRAINT chk_minimum_one_year CHECK (end_date >= start_date + INTERVAL '1 year'),
    -- Enforce year increments validation at API level
    CONSTRAINT chk_increment_years CHECK (
        EXTRACT(EPOCH FROM (end_date - start_date))::BIGINT % (365 * 24 * 60 * 60) = 0 
        OR EXTRACT(EPOCH FROM (end_date - start_date))::BIGINT % (366 * 24 * 60 * 60) = 0 -- accounting for leap years
    )
);

-- 5. Training Berth Request Table (Company requests slot quotas, DG Shipping approves)
CREATE TABLE public.training_berth_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vessel_id UUID NOT NULL REFERENCES public.vessel(id),
    requested_slots INTEGER NOT NULL DEFAULT 1,
    approved_slots INTEGER, -- approved slots by DG Shipping
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    concession_rate_per_day_usd NUMERIC(8, 2) NOT NULL DEFAULT 5.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Berth Seafarer Allocation Table (Tracks unique slots sequentially)
CREATE TABLE public.berth_seafarer_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    berth_id UUID NOT NULL REFERENCES public.berth(id),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
    berth_allocation_id UUID REFERENCES public.berth_allocation(id) ON DELETE SET NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_berth_seafarer_dates CHECK (end_date > start_date)
);

-- 7. Concession Credits Ledger Table (Brainstormed Feature: Concession tracking)
CREATE TABLE public.concession_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES public.company(id),
    vessel_id UUID NOT NULL REFERENCES public.vessel(id),
    berth_seafarer_allocation_id UUID NOT NULL REFERENCES public.berth_seafarer_allocation(id),
    cadet_days_logged INTEGER NOT NULL,
    concession_value_usd NUMERIC(12, 2) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_vessel_company_id ON public.vessel(company_id);
CREATE INDEX idx_berth_seafarer_allocation_berth_allocation_id ON public.berth_seafarer_allocation(berth_allocation_id);
CREATE INDEX idx_berth_seafarer_allocation_indos_master_id ON public.berth_seafarer_allocation(indos_master_id);
CREATE INDEX idx_concession_ledger_company ON public.concession_ledger(company_id);

-- Exclusion Constraints (Requires btree_gist extension)
ALTER TABLE public.berth_allocation
ADD CONSTRAINT no_overlapping_berth_allocation
EXCLUDE USING gist (
    berth_id WITH =,
    tstzrange(start_date, end_date) WITH &&
);

ALTER TABLE public.berth_seafarer_allocation
ADD CONSTRAINT no_overlapping_berth_seafarer_allocation
EXCLUDE USING gist (
    berth_id WITH =,
    tstzrange(start_date, end_date) WITH &&
);

ALTER TABLE public.berth_seafarer_allocation
ADD CONSTRAINT no_overlapping_seafarer_assignment
EXCLUDE USING gist (
    indos_master_id WITH =,
    tstzrange(start_date, end_date) WITH &&
);

-- Triggers
CREATE TRIGGER trg_company_update_at BEFORE UPDATE ON public.company FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_vessel_update_at BEFORE UPDATE ON public.vessel FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_berth_update_at BEFORE UPDATE ON public.berth FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_berth_allocation_update_at BEFORE UPDATE ON public.berth_allocation FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_berth_seafarer_allocation_update_at BEFORE UPDATE ON public.berth_seafarer_allocation FOR EACH ROW EXECUTE FUNCTION set_update_at();

-- Audit Triggers
CREATE TRIGGER trg_company_audit AFTER INSERT OR UPDATE OR DELETE ON public.company FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_vessel_audit AFTER INSERT OR UPDATE OR DELETE ON public.vessel FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_berth_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_berth_allocation_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth_allocation FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_berth_seafarer_allocation_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth_seafarer_allocation FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/companies` | `GET` | Authenticated | List all shipping companies. |
| `/api/v1/companies/{companyId}` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Request training berths for vessels. |
| `/api/v1/dgshipping/training-requests` | `GET` | `ROLE_DG_SHIPPING_ADMIN` | List pending training slot requests. |
| `/api/v1/dgshipping/training-requests/{reqId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN` | Approve/reject training slot request. |
| `/api/v1/companies/{companyId}/concessions` | `GET` | `ROLE_COMPANY_ADMIN`, `ROLE_DG_SHIPPING_ADMIN` | View company concession credits report. |
| `/api/v1/vessels` | `GET` | Authenticated | List all vessels globally. |
| `/api/v1/companies/{companyId}/vessels` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Register vessel to company. |
| `/api/v1/vessels/{vesselId}/crew-list` | `GET` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER`, `ROLE_DG_SHIPPING_ADMIN` | Generate standard IMO Crew List manifest. |
| `/api/v1/berth-allocations` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Book a berth slot for a vessel stay. Minimum 1 year. |
| `/api/v1/berth-seafarer-allocations` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Assign a seafarer to a berth slot (Gantt timeline entry). |

---

## 5. Endpoints & Sub-routes

### 5.1. Request Training Berth Allocation
- **Path**: `/api/v1/companies/{companyId}/training-requests`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "vesselId": "vessel-uuid",
    "requestedSlots": 5
  }
  ```
- **Response `201 Created`**: Returns created request with status `PENDING`.

### 5.2. Review Training Berth Request (DG Shipping)
- **Path**: `/api/v1/dgshipping/training-requests/{reqId}`
- **Method**: `PUT`
- **Body**:
  ```json
  {
    "status": "APPROVED",
    "approvedSlots": 4,
    "concessionRatePerDayUsd": 8.50
  }
  ```
- **Response `200 OK`**: Updated request details.

### 5.3. Get Concession Ledger
- **Path**: `/api/v1/companies/{companyId}/concessions`
- **Method**: `GET`
- **Response `200 OK`**:
  ```json
  {
    "companyId": "company-uuid",
    "totalCadetDaysLogged": 320,
    "totalConcessionCreditsUsd": 2720.00,
    "ledger": [
      {
        "vesselName": "Apex Voyager",
        "seafarerName": "John Doe",
        "cadetDaysLogged": 100,
        "concessionValueUsd": 850.00,
        "grantedAt": "2026-08-31T15:00:00Z"
      }
    ]
  }
  ```

### 5.4. Allocate Berth for Vessel (Vessel stay scheduling)
- **Path**: `/api/v1/berth-allocations`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "berthId": "berth-uuid",
    "vesselId": "vessel-uuid",
    "startDate": "2026-09-01T00:00:00Z",
    "endDate": "2027-09-01T00:00:00Z"
  }
  ```
- **Response `201 Created`**: Returns allocation details.
- **Constraints**: Will throw `400 Bad Request` if duration is not exactly matching years or less than 1 year.
