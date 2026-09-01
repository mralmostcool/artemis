// ─── Auth ────────────────────────────────────────────────────────────────────
export type Role =
	| 'DG_SHIPPING_ADMIN'
	| 'DG_SHIPPING_L1'
	| 'DG_SHIPPING_L2'
	| 'COMPANY_ADMIN'
	| 'COMPANY_USER'
	| 'INSTITUTE_ADMIN'
	| 'INSTITUTE_USER'
	| 'CANDIDATE';

export interface Organization {
	id: string;
	name: string;
}

export interface Profile {
	id: string;
	email: string;
	firstName: string;
	lastName: string;
	displayName: string;
	phoneNumber?: string;
	gender?: string;
	dateOfBirth?: string;
	avatarUrl?: string;
	role: Role;
	organizationId?: string;
	organizationName?: string;
	enabled: boolean;
}

export interface ProfileRequest {
	firstName?: string;
	lastName?: string;
	displayName: string;
	phoneNumber?: string;
	gender?: string;
	dateOfBirth?: string;
	avatarUrl?: string;
	role?: Role;
	organizationId?: string;
	organizationName?: string;
}

// ─── Seafarers ────────────────────────────────────────────────────────────────
export interface RankMaster {
	id: string;
	name: string;
	code?: string;
	category?: string;
}

export interface IndosMaster {
	id: string;
	indosNo: string;
	firstName: string;
	lastName: string;
	middleName?: string;
	dateOfBirth?: string;
	nationality?: string;
	rank?: RankMaster;
	active: boolean;
}

export interface ProfileIndosMapping {
	id: string;
	profile: Profile;
	indosMaster: IndosMaster;
	linkedAt: string;
}

export interface SeafarerMedical {
	id: string;
	indosMaster: IndosMaster;
	medicalType: string;
	issuedDate?: string;
	expiryDate?: string;
	issuedBy?: string;
	remarks?: string;
}

// ─── Certificate ─────────────────────────────────────────────────────────────
export type CertificateStatus = 'INITIATED' | 'L1_REVIEWED' | 'L2_APPROVED' | 'ALLOTTED';

export interface Certificate {
	id: string;
	contract: Contract;
	status: CertificateStatus;
	enqueuedAt?: string;
	l1Officer?: Profile;
	l1Remarks?: string;
	l1ReviewedAt?: string;
	l2Officer?: Profile;
	l2Remarks?: string;
	l2ApprovedAt?: string;
	allottedCompany?: Company;
	certificateNo?: string;
	expiryDate?: string;
	qrHash?: string;
}

// ─── Contracts ───────────────────────────────────────────────────────────────
export interface Contract {
	id: string;
	indosMaster: IndosMaster;
	vessel: Vessel;
	rank: RankMaster;
	plannedSignOnDate?: string;
	plannedSignOffDate?: string;
	actualSignOnDate?: string;
	actualSignOnPort?: string;
	actualSignOnCountry?: string;
	actualSignOffDate?: string;
	actualSignOffPort?: string;
	actualSignOffCountry?: string;
	signOffRemarks?: string;
	extendedSignOffDate?: string;
	wageUsd?: number;
	status?: string;
}

// ─── Vessels ─────────────────────────────────────────────────────────────────
export interface Company {
	id: string;
	name: string;
	imoCompanyNumber?: string;
	address?: string;
	country?: string;
	contactEmail?: string;
	contactPhone?: string;
}

export interface Vessel {
	id: string;
	company: Company;
	name: string;
	imoNumber?: string;
	flag?: string;
	type?: string;
	grossTonnage?: number;
	callSign?: string;
}

export interface Berth {
	id: string;
	name: string;
	location?: string;
	capacity: number;
}

export interface BerthAllocation {
	id: string;
	berth: Berth;
	vessel: Vessel;
	startDate: string;
	endDate: string;
}

export interface BerthSeafarerAllocation {
	id: string;
	berth: Berth;
	indosMaster: IndosMaster;
	berthAllocation: BerthAllocation;
	startDate: string;
	endDate: string;
}

export interface TrainingBerthRequest {
	id: string;
	vessel: Vessel;
	requestedSlots: number;
	approvedSlots?: number;
	concessionRate?: number;
	status?: string;
}

export interface ConcessionLedger {
	id: string;
	company: Company;
	credits: number;
	description?: string;
	createdAt?: string;
}

// ─── Institute ────────────────────────────────────────────────────────────────
export interface Institute {
	id: string;
	name: string;
	code?: string;
	address?: string;
	contactEmail?: string;
	contactPhone?: string;
	accreditationStatus?: string;
}

export type EnrollmentStatus = 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'DROPPED';

export interface PreSeaCourse {
	id: string;
	institute: Institute;
	name: string;
	code?: string;
	durationWeeks?: number;
	permittedCapacity?: number;
	status?: string;
	fee?: number;
}

export interface CoursePayment {
	id: string;
	enrollment: Enrollment;
	amount: number;
	currency?: string;
	reference?: string;
	paidAt?: string;
}

export interface Enrollment {
	id: string;
	course: PreSeaCourse;
	indosMaster: IndosMaster;
	status: EnrollmentStatus;
	enrolledAt?: string;
	attendance?: number;
	grade?: string;
	payment?: CoursePayment;
}

// ─── Payroll ─────────────────────────────────────────────────────────────────
export interface PaySlip {
	id: string;
	contract: Contract;
	periodStart: string;
	periodEnd: string;
	baseWageUsd?: number;
	targetCurrency?: string;
	convertedAmount?: number;
	status?: string;
	transactionReference?: string;
	paidAt?: string;
}
