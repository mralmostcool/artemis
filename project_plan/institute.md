# Maritime Training Institute (MTI) Module Specification

## 1. Purpose
The **Maritime Training Institute (MTI) Module** manages training academies, handles seat quota permissions issued by DG Shipping, plans preparatory pre-sea courses, and coordinates candidate checkout payment structures to manage enrollments.

---

## 2. Capabilities
- Manage directories of maritime training institutes (`institute`), including government training codes and approval details.
- Request seat quota permissions from DG Shipping; store permitted intake limits.
- Schedule pre-sea training courses (`pre_sea_courses`), defining code identifiers, duration, fees/costs, and permitted capacities.
- Provide mock checkout (`checkout`) and payment confirmation engines for candidates to register online.
- Track candidate enrollments through status flows: `APPLIED` $\rightarrow$ `PENDING_PAYMENT` $\rightarrow$ `ENROLLED` $\rightarrow$ `COMPLETED` / `CANCELLED`.
- Enforce constraints: Prevent double active enrollment for the same course.
- Lock enrollment modification if referenced by an active/sign-on contract (communicates with `contract` module).

---

## 3. Database Schema

Resides in the `public` schema. Uses postgres enum types, unique partial indices, and transaction ledgers.

```sql
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

-- 2. Pre-Sea Courses Table
CREATE TABLE public.pre_sea_courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    course_code VARCHAR(50) NOT NULL, -- e.g. GPR-01, DNS-02
    duration_days INTEGER NOT NULL DEFAULT 180,
    cost NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    
    -- Seat Quotas (Requested by MTI, approved by DG Shipping)
    requested_capacity INTEGER NOT NULL DEFAULT 40,
    permitted_capacity INTEGER DEFAULT NULL, -- Null indicates DG Shipping review pending
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

-- 5. Course Payments Table (Mock Checkout System)
CREATE TABLE public.course_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES public.enrollment(id),
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
CREATE TRIGGER trg_institute_update_at BEFORE UPDATE ON public.institute FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_pre_sea_courses_updated_at BEFORE UPDATE ON public.pre_sea_courses FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_enrollment_updated_at BEFORE UPDATE ON public.enrollment FOR EACH ROW EXECUTE FUNCTION set_update_at();

-- Audit triggers
CREATE TRIGGER trg_institute_audit AFTER INSERT OR UPDATE OR DELETE ON public.institute FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_pre_sea_courses_audit AFTER INSERT OR UPDATE OR DELETE ON public.pre_sea_courses FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_enrollment_audit AFTER INSERT OR UPDATE OR DELETE ON public.enrollment FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/institutes` | `GET` | Authenticated | View list of institutes. |
| `/api/v1/institutes` | `POST` | `ROLE_DG_SHIPPING_ADMIN` | Register a new MTI. |
| `/api/v1/institutes/{mtiId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_INSTITUTE_ADMIN` | Update institute details. |
| `/api/v1/courses` | `GET` | Authenticated | List all active courses globally. |
| `/api/v1/institutes/{mtiId}/courses` | `POST` | `ROLE_INSTITUTE_ADMIN`, `ROLE_INSTITUTE_USER` | Request course schedule & seat quota permission. |
| `/api/v1/courses/quotas` | `GET` | `ROLE_DG_SHIPPING_ADMIN` | List pending MTI seat quota requests. |
| `/api/v1/courses/quotas/{courseId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN` | Approve or Reject MTI course seat quota. |
| `/api/v1/courses/{courseId}/checkout` | `POST` | `ROLE_CANDIDATE` | Candidate mock checkout initiation (generates enrollment). |
| `/api/v1/payments/{paymentId}/confirm` | `POST` | `ROLE_CANDIDATE` | Complete payment mock transaction (triggers status $\rightarrow$ `ENROLLED`). |
| `/api/v1/enrollments/{enrollmentId}` | `PUT` | `ROLE_INSTITUTE_ADMIN`, `ROLE_INSTITUTE_USER` | Update enrollment status/remarks. |
| `/api/v1/candidates/{candidateId}/enrollments` | `GET` | Authenticated | View all enrollments for a candidate. |

---

## 5. Endpoints & Sub-routes

### 5.1. Request Seat Quota (MTI User)
- **Path**: `/api/v1/institutes/{mtiId}/courses`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "name": "Diploma in Nautical Science",
    "courseCode": "DNS-02",
    "durationDays": 180,
    "cost": 150000.00,
    "requestedCapacity": 40,
    "startDate": "2026-10-01"
  }
  ```
- **Response `201 Created`**: Course created with `quotaStatus` set to `PENDING`.

### 5.2. Approve/Reject Seat Quota (DG Shipping)
- **Path**: `/api/v1/courses/quotas/{courseId}`
- **Method**: `PUT`
- **Body**:
  ```json
  {
    "approved": true,
    "permittedCapacity": 40
  }
  ```
- **Response `200 OK`**: Course quota status updated.

### 5.3. Mock Checkout (Candidate)
- **Path**: `/api/v1/courses/{courseId}/checkout`
- **Method**: `POST`
- **Response `201 Created`**:
  ```json
  {
    "paymentId": "pay-uuid-9912",
    "enrollmentId": "enroll-uuid-2234",
    "amount": 150000.00,
    "currency": "INR",
    "status": "PENDING"
  }
  ```
- **Action**: Creates enrollment status `PENDING_PAYMENT`.

### 5.4. Confirm Payment Transaction (Mock Checkout)
- **Path**: `/api/v1/payments/{paymentId}/confirm`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "gatewayReference": "PAY-GATEWAY-REF-XYZ"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "enrollmentId": "enroll-uuid-2234",
    "status": "ENROLLED",
    "paymentStatus": "SUCCESS"
  }
  ```
- **Action**: Enrollment status changes to `ENROLLED`. Increments registered student count.
