# Seafarer & INDoS Module Specification

## 1. Purpose
The **Seafarer & INDoS Module** manages the physical registries of seafarers under the Indian National Database of Seafarers (INDoS) and controls the directory of ranks. It associates user identities (`CANDIDATE` role profiles) to their official maritime records. It also manages seafarer fitness status logs.

---

## 2. Capabilities
- Manage the master list of seafarer ranks (`rank_master`).
- Manage the central database of seafarer records (`indos_master`), including passport and Continuous Discharge Certificate (CDC) identifiers.
- Manage medical examination records (`seafarer_medical`) mapping physical fitness compliance.
- Allow lookup of seafarers by INDoS number for validation in training course enrollment and vessel sign-on.
- Associate/link a user's authenticated UUID with an `indos_master` record.

---

## 3. Database Schema

Resides in the `public` schema. Contains constraints and triggers for audit logging.

```sql
-- 1. Rank Master table
CREATE TABLE public.rank_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(64) NOT NULL UNIQUE,
    level INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. INDoS Master table
CREATE TABLE public.indos_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos VARCHAR(7) NOT NULL UNIQUE, -- Unique 7-character INDoS Number
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL DEFAULT '',
    rank_id UUID NOT NULL REFERENCES public.rank_master(id),
    
    -- Personal / Physical identifiers
    passport_no VARCHAR(50) UNIQUE,
    cdc_no VARCHAR(50) UNIQUE, -- Continuous Discharge Certificate Number
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    nationality VARCHAR(100) NOT NULL DEFAULT 'Indian',
    place_of_birth VARCHAR(100),
    blood_group VARCHAR(10),
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Profile-to-INDoS Mapping table (Decoupling Auth from Seafarer module)
CREATE TABLE public.profile_indos_mapping (
    profile_id UUID PRIMARY KEY, -- References public.profiles(id)
    indos_master_id UUID NOT NULL UNIQUE REFERENCES public.indos_master(id),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Seafarer Medical Examination Table (Brainstormed Feature: Fitness Compliance)
CREATE TABLE public.seafarer_medical (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indos_master_id UUID NOT NULL REFERENCES public.indos_master(id),
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

-- Trigger for updating updated_at
CREATE TRIGGER trg_indos_update_at BEFORE UPDATE ON public.indos_master FOR EACH ROW EXECUTE FUNCTION set_update_at();
CREATE TRIGGER trg_seafarer_medical_update_at BEFORE UPDATE ON public.seafarer_medical FOR EACH ROW EXECUTE FUNCTION set_update_at();

-- Trigger for Audit logs
CREATE TRIGGER trg_indos_master_audit AFTER INSERT OR UPDATE OR DELETE ON public.indos_master FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
CREATE TRIGGER trg_seafarer_medical_audit AFTER INSERT OR UPDATE OR DELETE ON public.seafarer_medical FOR EACH ROW EXECUTE FUNCTION audit_trigger_fn();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/seafarers/ranks` | `GET` | Authenticated | View list of ranks. |
| `/api/v1/seafarers/ranks` | `POST` | `ROLE_DG_SHIPPING_ADMIN` | Add new rank. |
| `/api/v1/seafarers/ranks/{rankId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN` | Update rank details. |
| `/api/v1/seafarers/ranks/{rankId}` | `DELETE` | `ROLE_DG_SHIPPING_ADMIN` | Remove a rank (fails if referenced). |
| `/api/v1/seafarers/indos` | `GET` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_COMPANY_USER`, `ROLE_INSTITUTE_ADMIN`, `ROLE_INSTITUTE_USER` | Query seafarer registry by search parameters. |
| `/api/v1/seafarers/indos` | `POST` | `ROLE_DG_SHIPPING_ADMIN` | Issue a new INDoS record. |
| `/api/v1/seafarers/indos/{indosId}` | `GET` | Authenticated | Get full details of specific INDoS record. |
| `/api/v1/seafarers/indos/{indosId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN` | Update INDoS record properties. |
| `/api/v1/seafarers/indos/{indosId}/status` | `PUT` | `ROLE_DG_SHIPPING_ADMIN` | Toggle INDoS active status. |
| `/api/v1/seafarers/link` | `GET` | Authenticated | View active mapping for user profile or specified user ID. |
| `/api/v1/seafarers/link` | `POST` | `ROLE_CANDIDATE` | Self-service mapping to associate own profile with INDoS number. |
| `/api/v1/seafarers/link/{profileId}` | `DELETE` | `ROLE_DG_SHIPPING_ADMIN` | Unlink profile mapping. |
| `/api/v1/seafarers/{indosId}/medicals` | `GET` | Authenticated | Fetch medical records of the seafarer. |
| `/api/v1/seafarers/{indosId}/medicals` | `POST` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN` | Add new medical fitness certification (logged by DG approved doctors). |

---

## 5. Endpoints & Sub-routes

### 5.1. List Ranks
- **Path**: `/api/v1/seafarers/ranks`
- **Method**: `GET`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "e5f6a1b2-7a8b-9c0d-1e2f-3a4b5c6d7e8f",
      "name": "Third Officer",
      "level": 4
    }
  ]
  ```

### 5.2. Create Rank
- **Path**: `/api/v1/seafarers/ranks`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "name": "Fourth Officer",
    "level": 3
  }
  ```
- **Response `201 Created`**: Created rank object.

### 5.3. Query Seafarers (Search)
- **Path**: `/api/v1/seafarers/indos`
- **Method**: `GET`
- **Query Params**: `indos=11AA123` or `firstName=John`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "indos": "11AA123",
      "firstName": "John",
      "lastName": "Doe",
      "passportNo": "L9981232",
      "cdcNo": "CDC-MUM-88122",
      "dateOfBirth": "1995-05-15",
      "bloodGroup": "O+",
      "rank": {
        "id": "e5f6a1b2-7a8b-9c0d-1e2f-3a4b5c6d7e8f",
        "name": "Third Officer",
        "level": 4
      },
      "isActive": true
    }
  ]
  ```

### 5.4. Create INDoS Record
- **Path**: `/api/v1/seafarers/indos`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "indos": "11AA123",
    "firstName": "John",
    "lastName": "Doe",
    "passportNo": "L9981232",
    "cdcNo": "CDC-MUM-88122",
    "dateOfBirth": "1995-05-15",
    "gender": "MALE",
    "bloodGroup": "O+",
    "rankId": "e5f6a1b2-7a8b-9c0d-1e2f-3a4b5c6d7e8f"
  }
  ```
- **Response `201 Created`**: Returns created INDoS master object.

### 5.5. Link Profile to INDoS
- **Path**: `/api/v1/seafarers/link`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "indos": "11AA123"
  }
  ```
- **Response `200 OK`**: Mapped profile details.

### 5.6. Fetch Medical Records
- **Path**: `/api/v1/seafarers/{indosId}/medicals`
- **Method**: `GET`
- **Response `200 OK`**:
  ```json
  [
    {
      "id": "m1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "doctorName": "Dr. Sarah Smith",
      "doctorRegistrationNo": "MCI-88231",
      "examinationDate": "2026-08-01",
      "expiryDate": "2027-08-01",
      "isFit": true,
      "remarks": "Fully fit for sea service duties."
    }
  ]
  ```

### 5.7. Create Medical Certificate
- **Path**: `/api/v1/seafarers/{indosId}/medicals`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "doctorName": "Dr. Sarah Smith",
    "doctorRegistrationNo": "MCI-88231",
    "examinationDate": "2026-08-01",
    "expiryDate": "2027-08-01",
    "isFit": true,
    "remarks": "Fully fit for sea service duties."
  }
  ```
- **Response `201 Created`**: Returns created medical record object.
