# Certificate Module Specification

## 1. Purpose
The **Certificate Module** manages the issuance and verification of professional maritime competency certificates. It implements a multi-level government sign-off workflow (DG Shipping Level 1 and Level 2) before allowing shipping companies to allot completed training certificates to candidates.

---

## 2. Capabilities
- Track certificate lifecycles through states: `INITIATED` $\rightarrow$ `REVIEWED_L1` $\rightarrow$ `APPROVED_L2` $\rightarrow$ `ALLOTTED`.
- Queue completed sea-service training contracts into the certificate queue.
- Implement a two-level sign-off workflow by DG Shipping officers.
- Block allotment of certificates to candidates until both levels of administrative sign-off are verified.
- Allow shipping companies to allot approved certificates to candidates.
- Generate secure verification signatures and QR code hashes for offline/external authentication.

---

## 3. Database Schema

Resides in the `public` schema.

```sql
-- 1. Certificate Status ENUM
CREATE TYPE public.certificate_status AS ENUM ('INITIATED', 'REVIEWED_L1', 'APPROVED_L2', 'ALLOTTED', 'REJECTED');

-- 2. Certificate Table
CREATE TABLE public.certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
    contract_id UUID NOT NULL REFERENCES public.contract(id) UNIQUE, -- Map to the completed sea service contract
    enrollment_id UUID NOT NULL REFERENCES public.enrollment(id),     -- Pre-sea course verification
    status public.certificate_status NOT NULL DEFAULT 'INITIATED',
    
    -- Certificate Parameters
    certificate_type VARCHAR(100) NOT NULL DEFAULT 'Certificate of Competency', -- 'COC', 'COP', etc.
    certificate_number VARCHAR(100) UNIQUE, -- Generated upon final allotment
    qr_code_hash VARCHAR(64) UNIQUE,        -- For secure verification scanner
    issue_date DATE,
    expiry_date DATE,                       -- Nullable for life certificates
    
    -- Verification Signatures
    l1_officer_id UUID REFERENCES public.profiles(id),
    l1_signed_at TIMESTAMP WITH TIME ZONE,
    l1_remarks TEXT,
    
    l2_officer_id UUID REFERENCES public.profiles(id),
    l2_signed_at TIMESTAMP WITH TIME ZONE,
    l2_remarks TEXT,
    
    allotted_by_company_id UUID REFERENCES public.company(id),
    allotted_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_certificates_indos_master_id ON public.certificates(indos_master_id);
CREATE INDEX idx_certificates_status ON public.certificates(status);

-- Trigger for updated_at & audit logs
CREATE TRIGGER trg_certificate_update_at BEFORE UPDATE ON public.certificates FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_certificate_audit AFTER INSERT OR UPDATE OR DELETE ON public.certificates FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
```

---

## 4. RBAC Rules

We introduce two new roles under the DG Shipping hierarchy:
- `ROLE_DG_SHIPPING_L1`: First-level review officer.
- `ROLE_DG_SHIPPING_L2`: Second-level final approval officer.

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/certificates` | `GET` | Authenticated | List certificates. |
| `/api/v1/certificates/{certId}` | `GET` | Authenticated | View detailed certificate, training, course, and review history. |
| `/api/v1/certificates/verify/{qrHash}` | `GET` | **Public** | Public verification portal: verify validity via QR hash. |
| `/api/v1/certificates` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Initiate a certificate request for a completed contract. |
| `/api/v1/certificates/{certId}/review/l1` | `POST` | `ROLE_DG_SHIPPING_L1` | Level 1 review: Approve or reject. |
| `/api/v1/certificates/{certId}/review/l2` | `POST` | `ROLE_DG_SHIPPING_L2` | Level 2 review: Final sign-off or reject. |
| `/api/v1/certificates/{certId}/allot` | `POST` | `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER` | Allot the fully approved certificate to the candidate. |

---

## 5. Endpoints & Sub-routes

### 5.1. List Certificates
- **Path**: `/api/v1/certificates`
- **Method**: `GET`
- **Query Params**: `status=INITIATED`, `indosMasterId=...` (Optional)
- **Response `200 OK`**: Array of certificate records.

### 5.2. View Certificate Details
- **Path**: `/api/v1/certificates/{certId}`
- **Method**: `GET`
- **Response `200 OK`**: Details including contract, courses, status, L1/L2 reviews.

### 5.3. Public Verification
- **Path**: `/api/v1/certificates/verify/{qrHash}`
- **Method**: `GET`
- **Response `200 OK`**:
  ```json
  {
    "status": "VALID",
    "certificateNumber": "CERT-2026-991823",
    "certificateType": "Certificate of Competency",
    "seafarerName": "John Doe",
    "indos": "11AA123",
    "issuedBy": "Apex Shipping Line",
    "issueDate": "2026-08-31",
    "expiryDate": "2031-08-31"
  }
  ```

### 5.4. Initiate Certificate Request
- **Path**: `/api/v1/certificates`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "contractId": "contract-uuid",
    "certificateType": "COC"
  }
  ```
- **Response `201 Created`**: Returns initiated certificate record.

### 5.5. Level 1 Officer Sign-Off
- **Path**: `/api/v1/certificates/{certId}/review/l1`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "approved": true,
    "remarks": "Sea-service hours and pre-sea courses verified correct."
  }
  ```
- **Response `200 OK`**: Updated certificate with status `REVIEWED_L1` or `REJECTED`.

### 5.6. Level 2 Officer Final Sign-Off
- **Path**: `/api/v1/certificates/{certId}/review/l2`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "approved": true,
    "remarks": "Reviewed and verified. Ready for allotment."
  }
  ```
- **Response `200 OK`**: Updated certificate with status `APPROVED_L2` or `REJECTED`.

### 5.7. Allot Certificate (Shipping Company)
- **Path**: `/api/v1/certificates/{certId}/allot`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "expiryDate": "2031-08-31"
  }
  ```
- **Response `200 OK`**: Returns allotted certificate with generated number and QR hash.
