-- 1. Certificate Status ENUM
CREATE TYPE public.certificate_status AS ENUM ('INITIATED', 'REVIEWED_L1', 'APPROVED_L2', 'ALLOTTED', 'REJECTED');

-- 2. Certificate Table
CREATE TABLE public.certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES public.contract(id) UNIQUE,
    enrollment_id UUID NOT NULL REFERENCES public.enrollment(id),
    status public.certificate_status NOT NULL DEFAULT 'INITIATED',
    
    -- Certificate Parameters
    certificate_type VARCHAR(100) NOT NULL DEFAULT 'Certificate of Competency',
    certificate_number VARCHAR(100) UNIQUE,
    qr_code_hash VARCHAR(64) UNIQUE,
    issue_date DATE,
    expiry_date DATE,
    
    -- Verification Signatures
    l1_officer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    l1_signed_at TIMESTAMP WITH TIME ZONE,
    l1_remarks TEXT,
    
    l2_officer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    l2_signed_at TIMESTAMP WITH TIME ZONE,
    l2_remarks TEXT,
    
    allotted_by_company_id UUID REFERENCES public.company(id) ON DELETE SET NULL,
    allotted_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_certificates_indos_master_id ON public.certificates(indos_master_id);
CREATE INDEX idx_certificates_status ON public.certificates(status);

-- Trigger for updated_at & audit logs
CREATE TRIGGER trg_certificate_update_at BEFORE UPDATE ON public.certificates FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_certificate_audit AFTER INSERT OR UPDATE OR DELETE ON public.certificates FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
