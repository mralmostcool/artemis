-- 1. Helper function for updated_at trigger if not exists
CREATE OR REPLACE FUNCTION public.set_update_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Audit Logs table and trigger function
CREATE TABLE public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    record_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by UUID,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION public.audit_trigger_fn()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.audit_logs(table_name, operation, record_id, old_values, new_values, changed_by)
    VALUES (
        TG_TABLE_NAME,
        TG_OP,
        COALESCE(NEW.id, OLD.id),
        CASE WHEN TG_OP != 'INSERT' THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP != 'DELETE' THEN to_jsonb(NEW) ELSE NULL END,
        NULLIF(current_setting('app.current_user_id', true), '')::UUID
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 3. Rank Master Table
CREATE TABLE public.rank_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    level INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Ranks
INSERT INTO public.rank_master (name, level) VALUES
    ('Deck Cadet', 1),
    ('Ordinary Seaman', 2),
    ('Able Seaman', 3),
    ('Third Officer', 4),
    ('Second Officer', 5),
    ('Chief Officer', 6),
    ('Master', 7);

-- 4. INDoS Master Table
CREATE TABLE public.indos_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos VARCHAR(7) NOT NULL UNIQUE, -- Unique 7-character INDoS Number
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL DEFAULT '',
    rank_id UUID NOT NULL REFERENCES public.rank_master(id),
    
    -- Personal / Physical identifiers
    passport_no VARCHAR(50) UNIQUE,
    cdc_no VARCHAR(50) UNIQUE,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    nationality VARCHAR(100) NOT NULL DEFAULT 'Indian',
    place_of_birth VARCHAR(100),
    blood_group VARCHAR(10),
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Profile-to-INDoS Mapping table
CREATE TABLE public.profile_indos_mapping (
    profile_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    indos_master_id UUID NOT NULL UNIQUE REFERENCES public.indos_master(id),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Seafarer Medical Examination Table
CREATE TABLE public.seafarer_medical (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id) ON DELETE CASCADE,
    doctor_name VARCHAR(255) NOT NULL,
    doctor_registration_no VARCHAR(100) NOT NULL,
    examination_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    is_fit BOOLEAN NOT NULL DEFAULT TRUE,
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_medical_dates CHECK (expiry_date > examination_date)
);

-- Indices
CREATE INDEX idx_indos_master_rank_id ON public.indos_master(rank_id);
CREATE INDEX idx_indos_master_num ON public.indos_master(indos);
CREATE INDEX idx_seafarer_medical_indos ON public.seafarer_medical(indos_master_id);

-- Triggers for updated_at
CREATE TRIGGER trg_indos_update_at BEFORE UPDATE ON public.indos_master FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_seafarer_medical_update_at BEFORE UPDATE ON public.seafarer_medical FOR EACH ROW EXECUTE FUNCTION public.set_update_at();

-- Triggers for Audits
CREATE TRIGGER trg_indos_master_audit AFTER INSERT OR UPDATE OR DELETE ON public.indos_master FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_seafarer_medical_audit AFTER INSERT OR UPDATE OR DELETE ON public.seafarer_medical FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
