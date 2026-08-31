# Payroll Module Specification

## 1. Purpose
The **Payroll Module** coordinates monthly wage disbursements for seafarers active on sea service contracts. It supports multi-currency conversions using mock exchange rates, handles payroll processing by shipping companies, and provides pay slip registries for seafarers.

---

## 2. Capabilities
- Process monthly payroll runs for active contracts (`ACTIVE` status).
- Convert base contract wages (denominated in USD) to target local currencies (e.g. INR, EUR, SGD) based on dynamic transaction exchange rates.
- Register generated pay slips in a secure ledger.
- Provide seafarers with download/view access to their salary receipts.

---

## 3. Database Schema

Resides in the `public` schema.

```sql
-- 1. Pay Slip Table
CREATE TABLE public.pay_slips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES public.contract(id),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
    company_id UUID NOT NULL REFERENCES public.company(id),
    
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    
    base_salary_usd NUMERIC(10, 2) NOT NULL,
    exchange_rate NUMERIC(12, 6) NOT NULL DEFAULT 1.000000,
    target_currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payout_amount NUMERIC(12, 2) NOT NULL, -- computed as base_salary_usd * exchange_rate
    
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
CREATE TRIGGER trg_payslip_update_at BEFORE UPDATE ON public.pay_slips FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_payslip_audit AFTER INSERT OR UPDATE OR DELETE ON public.pay_slips FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/payroll/slips` | `GET` | Authenticated | View list of payslips. Candidates see own; Companies see their processed runs. |
| `/api/v1/payroll/slips/{slipId}` | `GET` | Authenticated | View details of a specific pay slip. |
| `/api/v1/payroll/process` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Trigger monthly payroll run for company contracts. |
| `/api/v1/payroll/slips/{slipId}/disburse` | `POST` | `ROLE_COMPANY_ADMIN` | Mark payslip as paid with transaction reference. |

---

## 5. Endpoints & Sub-routes

### 5.1. List Pay Slips
- **Path**: `/api/v1/payroll/slips`
- **Method**: `GET`
- **Query Params**: `indosMasterId=...`, `companyId=...` (Optional)
- **Response `200 OK`**: Array of pay slip objects.

### 5.2. View Pay Slip
- **Path**: `/api/v1/payroll/slips/{slipId}`
- **Method**: `GET`
- **Response `200 OK`**:
  ```json
  {
    "id": "p1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "contractId": "contract-uuid",
    "seafarer": {
      "indos": "11AA123",
      "name": "John Doe"
    },
    "payPeriod": {
      "start": "2026-08-01",
      "end": "2026-08-31"
    },
    "wages": {
      "baseUsd": 3500.00,
      "exchangeRate": 83.450000,
      "targetCurrency": "INR",
      "payoutAmount": 292075.00
    },
    "status": "PAID",
    "paidAt": "2026-09-01T10:00:00Z",
    "transactionReference": "TXN-88122-MUM"
  }
  ```

### 5.3. Process Monthly Payroll
- **Path**: `/api/v1/payroll/process`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "payPeriodStart": "2026-08-01",
    "payPeriodEnd": "2026-08-31",
    "targetCurrency": "INR"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "processedCount": 14,
    "totalPayoutValue": 4089050.00,
    "slipsCreated": [
      "slip-uuid-1",
      "slip-uuid-2"
    ]
  }
  ```

### 5.4. Disburse Payment
- **Path**: `/api/v1/payroll/slips/{slipId}/disburse`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "transactionReference": "TXN-88122-MUM"
  }
  ```
- **Response `200 OK`**: Updated pay slip object with status `PAID`.
