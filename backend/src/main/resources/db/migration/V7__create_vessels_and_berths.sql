-- 1. Company Table (Shipping Company)
CREATE TABLE public.company (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_no VARCHAR(64) UNIQUE,
    rpsl_no VARCHAR(100) UNIQUE, -- Recruitment & Placement Services License
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
    imo VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    flag VARCHAR(64) NOT NULL,
    vessel_type VARCHAR(100) NOT NULL DEFAULT 'Bulk Carrier',
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

-- 3. Berth Table (Port Physical Berth)
CREATE TABLE public.berth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    berth_name VARCHAR(128) NOT NULL UNIQUE,
    max_draft_meters NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    max_loa_meters NUMERIC(6, 2) NOT NULL DEFAULT 200.00,
    coordinate_x NUMERIC(10, 6),
    coordinate_y NUMERIC(10, 6),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Berth Allocation Table (Vessel stay scheduling)
CREATE TABLE public.berth_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    berth_id UUID NOT NULL REFERENCES public.berth(id),
    vessel_id UUID NOT NULL REFERENCES public.vessel(id),
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_berth_alloc_dates CHECK (end_date > start_date),
    CONSTRAINT chk_minimum_one_year CHECK (end_date >= start_date + INTERVAL '1 year')
);

-- 5. Training Berth Request Table (Company request slot limits, DG Shipping approves)
CREATE TABLE public.training_berth_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vessel_id UUID NOT NULL REFERENCES public.vessel(id) ON DELETE CASCADE,
    requested_slots INTEGER NOT NULL DEFAULT 1,
    approved_slots INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    concession_rate_per_day_usd NUMERIC(8, 2) NOT NULL DEFAULT 5.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Berth Seafarer Allocation Table (Unique slots allocated sequentially)
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

-- 7. Concession Ledger Table
CREATE TABLE public.concession_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES public.company(id) ON DELETE CASCADE,
    vessel_id UUID NOT NULL REFERENCES public.vessel(id) ON DELETE CASCADE,
    berth_seafarer_allocation_id UUID NOT NULL REFERENCES public.berth_seafarer_allocation(id) ON DELETE CASCADE,
    cadet_days_logged INTEGER NOT NULL,
    concession_value_usd NUMERIC(12, 2) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_vessel_company_id ON public.vessel(company_id);
CREATE INDEX idx_berth_seafarer_allocation_berth_allocation_id ON public.berth_seafarer_allocation(berth_allocation_id);
CREATE INDEX idx_berth_seafarer_allocation_indos_master_id ON public.berth_seafarer_allocation(indos_master_id);
CREATE INDEX idx_concession_ledger_company ON public.concession_ledger(company_id);

-- Exclusion Constraints
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

-- Triggers for updated_at
CREATE TRIGGER trg_company_update_at BEFORE UPDATE ON public.company FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_vessel_update_at BEFORE UPDATE ON public.vessel FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_berth_update_at BEFORE UPDATE ON public.berth FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_berth_allocation_update_at BEFORE UPDATE ON public.berth_allocation FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_berth_seafarer_allocation_update_at BEFORE UPDATE ON public.berth_seafarer_allocation FOR EACH ROW EXECUTE FUNCTION public.set_update_at();

-- Triggers for Audits
CREATE TRIGGER trg_company_audit AFTER INSERT OR UPDATE OR DELETE ON public.company FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_vessel_audit AFTER INSERT OR UPDATE OR DELETE ON public.vessel FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_berth_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_berth_allocation_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth_allocation FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_berth_seafarer_allocation_audit AFTER INSERT OR UPDATE OR DELETE ON public.berth_seafarer_allocation FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
