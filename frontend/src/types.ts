// Miroir des DTO du backend (io.noba.web.dto).

export type Role = "SUPER_ADMIN" | "ORG_ADMIN" | "AGENT";
export type TicketStatus = "WAITING" | "CALLED" | "SERVING" | "DONE" | "NO_SHOW" | "CANCELLED" | "EXPIRED";

// ---- Public ----
export interface ServicePublic {
  id: number;
  name: string;
  description?: string;
  prefix: string;
  waiting: number;
  estimatedWaitMinutes: number;
  available: boolean;
}

export interface BranchPublic {
  code: string;
  name: string;
  organizationName: string;
  address?: string;
  open: boolean;
  services: ServicePublic[];
}

export interface TicketView {
  token: string;
  code: string;
  status: TicketStatus;
  serviceName: string;
  branchName: string;
  branchCode: string;
  organizationName: string;
  peopleAhead?: number;
  estimatedWaitMinutes?: number;
  counterName?: string;
  createdAt: string;
  calledAt?: string;
  rating?: number;
}

export interface CalledTicket {
  code: string;
  counterName?: string;
  status: TicketStatus;
  calledAt: string;
}

export interface DisplayView {
  branchName: string;
  organizationName: string;
  called: CalledTicket[];
  queues: { name: string; prefix: string; waiting: number }[];
}

export interface QueueEvent {
  type: string;
  ticketCode?: string;
  counterName?: string;
}

// ---- Auth ----
export interface Me {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  organizationId?: number;
  organizationName?: string;
  branchId?: number;
}

export interface AuthResponse {
  token: string;
  user: Me;
}

// ---- Agent ----
export interface BranchSummary {
  id: number;
  name: string;
  code: string;
  open: boolean;
}

export interface AgentTicket {
  id: number;
  code: string;
  status: TicketStatus;
  serviceId: number;
  serviceName: string;
  queuedAt: string;
  calledAt?: string;
  startedAt?: string;
  recallCount: number;
}

export interface CounterView {
  id: number;
  name: string;
  active: boolean;
  agentId?: number;
  agentName?: string;
  serviceIds: number[];
  current?: AgentTicket;
}

export interface ServiceBoard {
  id: number;
  name: string;
  prefix: string;
  waiting: number;
  estimatedWaitMinutes: number;
  avgServiceMinutes: number;
}

export interface BoardView {
  branch: BranchSummary;
  services: ServiceBoard[];
  counters: CounterView[];
  waiting: AgentTicket[];
  recent: CalledTicket[];
}

// ---- Admin ----
export interface BranchAdmin {
  id: number;
  name: string;
  code: string;
  address?: string;
  open: boolean;
}

export interface ServiceAdmin {
  id: number;
  name: string;
  description?: string;
  prefix: string;
  defaultServiceMinutes: number;
  dailyLimit?: number;
  active: boolean;
  sortOrder: number;
}

export interface CounterAdmin {
  id: number;
  name: string;
  active: boolean;
  serviceIds: number[];
  agentName?: string;
}

export interface StaffMember {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  branchId?: number;
  branchName?: string;
  active: boolean;
}

export interface Stats {
  total: number;
  served: number;
  noShow: number;
  cancelled: number;
  waiting: number;
  avgWaitMinutes?: number;
  avgServiceMinutes?: number;
  avgRating?: number;
  ratings: number;
  byHour: number[];
  byService: { name: string; total: number; served: number; avgWaitMinutes?: number }[];
  byAgent: { name: string; served: number; avgServiceMinutes?: number }[];
}

export interface OrganizationAdmin {
  id: number;
  name: string;
  slug: string;
  active: boolean;
  createdAt: string;
  branches: number;
  staff: number;
}
