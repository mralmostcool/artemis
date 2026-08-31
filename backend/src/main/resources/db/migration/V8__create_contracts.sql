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
    agreement_type VARCHAR(100) NOT NULL DEFAULT 'ITF Standard CBA',
    rpsl_no VARCHAR(100) NOT NULL, -- copied from company details
    
    -- Next of Kin details
    next_of_kin_name VARCHAR(255) NOT NULL,
    next_of_kin_relation VARCHAR(100) NOT NULL,
    next_of_kin_phone VARCHAR(50) NOT NULL,

    -- Planned logistics
    sign_on_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sign_on_port VARCHAR(128) NOT NULL,
    sign_on_country VARCHAR(128) NOT NULL,
    sign_off_date TIMESTAMP WITH TIME ZONE NOT NULL,
    sign_off_port VARCHAR(128) NOT NULL,
    sign_off_country VARCHAR(128) NOT NULL,

    -- Actual logistics
    actual_sign_on_date TIMESTAMP WITH TIME ZONE,
    actual_sign_on_port VARCHAR(128),
    actual_sign_on_country VARCHAR(128),
    actual_sign_off_date TIMESTAMP WITH TIME ZONE,
    actual_sign_off_port VARCHAR(128),
    actual_sign_off_country VARCHAR(128),

    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraint checks
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
CREATE OR REPLACE FUNCTION public.check_enrollment_completed()
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
FOR EACH ROW EXECUTE FUNCTION public.check_enrollment_completed();

-- Locking trigger on public.enrollment to lock status changes if referenced
CREATE OR REPLACE FUNCTION public.check_no_contract_depends_on_enrollment()
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
FOR EACH ROW EXECUTE FUNCTION public.check_no_contract_depends_on_enrollment();

-- Triggers for updated_at & audit log
CREATE TRIGGER trg_contract_update_at BEFORE UPDATE ON public.contract FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_contract_audit AFTER INSERT OR UPDATE OR DELETE ON public.contract FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
