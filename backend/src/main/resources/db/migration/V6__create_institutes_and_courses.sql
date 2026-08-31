-- 1. Institute Table
CREATE TABLE public.institute (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    mti_code VARCHAR(50) NOT NULL UNIQUE, -- official DG shipping institute identifier code
    address VARCHAR(512),
    city VARCHAR(100),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    website VARCHAR(255),
    approval_date DATE,
    approval_expiry_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Pre-Sea Courses Table (with seat quota metadata)
CREATE TABLE public.pre_sea_courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    course_code VARCHAR(50) NOT NULL, -- e.g. GPR-01, DNS-02
    duration_days INTEGER NOT NULL DEFAULT 180,
    cost NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    
    -- Seat Quotas
    requested_capacity INTEGER NOT NULL DEFAULT 40,
    permitted_capacity INTEGER DEFAULT NULL, -- approved limit (Null = pending)
    quota_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE NOT NULL,
    institute_id UUID NOT NULL REFERENCES public.institute(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_course_code_per_mti UNIQUE (course_code, institute_id)
);

-- 3. Enrollment Status ENUM
CREATE TYPE public.enrollment_status AS ENUM ('APPLIED', 'PENDING_PAYMENT', 'ENROLLED', 'COMPLETED', 'CANCELLED');

-- 4. Enrollment Table
CREATE TABLE public.enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pre_sea_course_id UUID NOT NULL REFERENCES public.pre_sea_courses(id),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
    status public.enrollment_status NOT NULL DEFAULT 'APPLIED',
    
    -- Academic Details
    roll_no VARCHAR(50),
    attendance_percentage NUMERIC(5, 2),
    grade VARCHAR(10),
    certificate_issued BOOLEAN NOT NULL DEFAULT FALSE,
    remarks TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Course Payments Table (Mock Checkout)
CREATE TABLE public.course_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES public.enrollment(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SUCCESS', 'FAILED'
    gateway_reference VARCHAR(100),
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_pre_sea_courses_institute_id ON public.pre_sea_courses(institute_id);
CREATE INDEX idx_enrollment_pre_sea_course_id ON public.enrollment(pre_sea_course_id);
CREATE INDEX idx_enrollment_indos_master_id ON public.enrollment(indos_master_id);
CREATE INDEX idx_course_payments_enrollment ON public.course_payments(enrollment_id);

-- Partial Unique Index (Candidate can have only one active ENROLLED status per course)
CREATE UNIQUE INDEX idx_enrollment_unique_active
ON public.enrollment(pre_sea_course_id, indos_master_id)
WHERE status = 'ENROLLED';

-- Triggers for updated_at
CREATE TRIGGER trg_institute_update_at BEFORE UPDATE ON public.institute FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_pre_sea_courses_updated_at BEFORE UPDATE ON public.pre_sea_courses FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_enrollment_updated_at BEFORE UPDATE ON public.enrollment FOR EACH ROW EXECUTE FUNCTION public.set_update_at();

-- Triggers for Audits
CREATE TRIGGER trg_institute_audit AFTER INSERT OR UPDATE OR DELETE ON public.institute FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_pre_sea_courses_audit AFTER INSERT OR UPDATE OR DELETE ON public.pre_sea_courses FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
CREATE TRIGGER trg_enrollment_audit AFTER INSERT OR UPDATE OR DELETE ON public.enrollment FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
