# Auth & Identity Module Specification

## 1. Purpose
The **Auth & Identity Module** manages identities, authentication tokens, system-wide organization directory, and user profile metadata. It acts as the security gateway for all other modules in the system.

---

## 2. Capabilities
- Authenticate and validate JWTs signed by Supabase.
- Manage user profiles with associated personal metadata.
- Support multi-tenancy via unified Organizations (`DG_SHIPPING`, `COMPANY`, `INSTITUTE`).
- Provide Role-Based Access Control (RBAC) verification on API level.
- Support administrative operations for enabling/disabling user logins.

---

## 3. Database Schema

The schema resides in the `public` schema. It links directly to the Supabase security table `auth.users`.

```sql
-- 1. Organizations table
CREATE TABLE public.organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL DEFAULT 'COMPANY', -- 'DG_SHIPPING', 'COMPANY', 'INSTITUTE'
    license_no VARCHAR(100), -- government license details (e.g. RPSL or MTI approval number)
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Profiles table
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    organization_id INT REFERENCES public.organizations(id) ON DELETE SET NULL,
    role VARCHAR(50) NOT NULL, -- 'DG_SHIPPING_ADMIN', 'DG_SHIPPING_L1', 'DG_SHIPPING_L2', 'COMPANY_ADMIN', 'COMPANY_USER', 'INSTITUTE_ADMIN', 'INSTITUTE_USER', 'CANDIDATE'
    email VARCHAR(255) UNIQUE,
    first_name VARCHAR(100) NOT NULL DEFAULT '',
    last_name VARCHAR(100) NOT NULL DEFAULT '',
    display_name VARCHAR(255) NOT NULL DEFAULT '',
    phone_number VARCHAR(50),
    gender VARCHAR(20),
    date_of_birth DATE,
    avatar_url VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_profiles_organization_id ON public.profiles(organization_id);
CREATE INDEX idx_profiles_role ON public.profiles(role);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON public.organizations FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
```

---

## 4. RBAC Rules

| Endpoint Path | Method | Required Roles | Description |
| :--- | :--- | :--- | :--- |
| `/api/v1/auth` | `GET` | Authenticated | Fetch current user's profile and organization metadata. |
| `/api/v1/auth` | `POST` | Authenticated | Create a profile on registration (self-service). |
| `/api/v1/auth` | `PUT` | Authenticated | Update self display name and phone number. |
| `/api/v1/auth/users` | `GET` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_INSTITUTE_ADMIN` | List all users (Admins see organization scope; DG Shipping Admin sees globally). |
| `/api/v1/auth/users/{userId}` | `GET` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_INSTITUTE_ADMIN` | Fetch detailed profile of any user in scope. |
| `/api/v1/auth/users/{userId}/role` | `PUT` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_INSTITUTE_ADMIN` | Update role of a managed user. |
| `/api/v1/auth/users/{userId}/status` | `PUT` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_INSTITUTE_ADMIN` | Enable or disable a user within their organization boundary. |
| `/api/v1/auth/organizations` | `GET` | `ROLE_DG_SHIPPING_ADMIN` | List all organizations globally. |
| `/api/v1/auth/organizations/{orgId}` | `GET` | Authenticated | View details of specific organization. |
| `/api/v1/auth/organizations` | `POST` | `ROLE_DG_SHIPPING_ADMIN` | Register new organization. |
| `/api/v1/auth/organizations/{orgId}` | `PUT` | `ROLE_DG_SHIPPING_ADMIN`, `ROLE_COMPANY_ADMIN`, `ROLE_INSTITUTE_ADMIN` | Update organization details. |

---

## 5. Endpoints & Sub-routes

### 5.1. Get Current Profile
- **Path**: `/api/v1/auth`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response `200 OK`**:
  ```json
  {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "displayName": "John Doe",
    "phoneNumber": "+919876543210",
    "gender": "MALE",
    "dateOfBirth": "1995-05-15",
    "role": "COMPANY_ADMIN",
    "organizationId": 2,
    "organizationName": "Apex Shipping Line",
    "organizationType": "COMPANY",
    "enabled": true
  }
  ```

### 5.2. Register Profile
- **Path**: `/api/v1/auth`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <JWT>`
- **Body**:
  ```json
  {
    "firstName": "John",
    "lastName": "Doe",
    "displayName": "John Doe",
    "phoneNumber": "+919876543210",
    "gender": "MALE",
    "dateOfBirth": "1995-05-15",
    "organizationId": 2,
    "role": "COMPANY_USER"
  }
  ```
- **Response `201 Created`**: Returns registered profile.

### 5.3. Update Profile
- **Path**: `/api/v1/auth`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <JWT>`
- **Body**:
  ```json
  {
    "firstName": "Johnathan",
    "lastName": "Doe",
    "displayName": "John Updated",
    "phoneNumber": "+919999999999",
    "avatarUrl": "https://example.com/avatar.jpg"
  }
  ```
- **Response `200 OK`**: Returns updated profile.

### 5.4. List Users (Scoped)
- **Path**: `/api/v1/auth/users`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <JWT>`
- **Query Params**: `role=COMPANY_USER`, `enabled=true`, `search=John` (Optional)
- **Response `200 OK`**: Array of user profiles matching criteria.

### 5.5. View Specific User
- **Path**: `/api/v1/auth/users/{userId}`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response `200 OK`**: User profile details.

### 5.6. Update Managed User Role
- **Path**: `/api/v1/auth/users/{userId}/role`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <JWT>`
- **Body**:
  ```json
  {
    "role": "COMPANY_USER"
  }
  ```
- **Response `200 OK`**: Updated user profile.

### 5.7. Toggle User Status
- **Path**: `/api/v1/auth/users/{userId}/status`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <JWT>`
- **Query Params**: `enabled=false`
- **Rules**:
  - `DG_SHIPPING_ADMIN` can toggle any user.
  - `COMPANY_ADMIN`/`INSTITUTE_ADMIN` can only toggle users belonging to the same `organizationId`. Cannot disable self.
- **Response `200 OK`**: Returns updated target profile.

### 5.8. List Organizations (Global)
- **Path**: `/api/v1/auth/organizations`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <JWT>`
- **Query Params**: `type=COMPANY` (Optional)
- **Response `200 OK`**: Array of all organizations.

### 5.9. View Specific Organization
- **Path**: `/api/v1/auth/organizations/{orgId}`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <JWT>`
- **Response `200 OK`**: Organization details.

### 5.10. Create Organization
- **Path**: `/api/v1/auth/organizations`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <JWT>`
- **Body**:
  ```json
  {
    "name": "Global Maritime Training",
    "type": "INSTITUTE",
    "licenseNo": "MTI-DG-88122",
    "contactEmail": "info@globalmarine.org",
    "contactPhone": "+91112345678",
    "addressLine1": "101 Ocean Drive",
    "city": "Mumbai",
    "state": "Maharashtra",
    "country": "India",
    "postalCode": "400001"
  }
  ```
- **Response `201 Created`**: Returns created organization.

### 5.11. Update Organization
- **Path**: `/api/v1/auth/organizations/{orgId}`
- **Method**: `PUT`
- **Headers**: `Authorization: Bearer <JWT>`
- **Body**:
  ```json
  {
    "name": "Updated Organization Name",
    "contactEmail": "newemail@org.com",
    "contactPhone": "+9199998888"
  }
  ```
- **Response `200 OK`**: Updated organization object.
