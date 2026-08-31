-- 1. Pay Slip Table
CREATE TABLE public.pay_slips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES public.contract(id) ON DELETE CASCADE,
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES public.company(id) ON DELETE CASCADE,
    
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    
    base_salary_usd NUMERIC(10, 2) NOT NULL,
    exchange_rate NUMERIC(12, 6) NOT NULL DEFAULT 1.000000,
    target_currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payout_amount NUMERIC(12, 2) NOT NULL,
    
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'FAILED'
    paid_at TIMESTAMP WITH TIME ZONE,
    transaction_reference VARCHAR(100),
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_period_dates CHECK (pay_period_end >= pay_period_start)
);

-- Indices
CREATE INDEX idx_pay_slips_contract_id ON public.pay_slips(contract_id);
CREATE INDEX idx_pay_slips_indos_master_id ON public.pay_slips(indos_master_id);
CREATE INDEX idx_pay_slips_company_id ON public.pay_slips(company_id);

-- Triggers for updated_at & audit log
CREATE TRIGGER trg_payslip_update_at BEFORE UPDATE ON public.pay_slips FOR EACH ROW EXECUTE FUNCTION public.set_update_at();
CREATE TRIGGER trg_payslip_audit AFTER INSERT OR UPDATE OR DELETE ON public.pay_slips FOR EACH ROW EXECUTE FUNCTION public.audit_trigger_fn();
