export const TICKET_STATUSES = [
  'ABERTO',
  'EM_ANDAMENTO',
  'CONCLUIDO',
  'FECHADO',
] as const

export type TicketStatus = (typeof TICKET_STATUSES)[number]

export const STATUS_LABELS: Record<TicketStatus, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  FECHADO: 'Fechado',
}

export const USER_SECTORS = [
  'TI',
  'ADMINISTRACAO',
  'FINANCEIRO',
  'FATURAMENTO',
  'NUTRICAO',
  'ALMOXARIFADO',
] as const

export type UserSector = (typeof USER_SECTORS)[number]

export const SECTOR_LABELS: Record<UserSector, string> = {
  TI: 'Tecnologia da informação',
  ADMINISTRACAO: 'Administração',
  FINANCEIRO: 'Financeiro',
  FATURAMENTO: 'Faturamento',
  NUTRICAO: 'Nutrição',
  ALMOXARIFADO: 'Almoxarifado',
}

export const USER_ROLES = ['TECNICO', 'SOLICITANTE'] as const

export type UserRole = (typeof USER_ROLES)[number]

export const ROLE_LABELS: Record<UserRole, string> = {
  TECNICO: 'Técnico',
  SOLICITANTE: 'Solicitante',
}

export interface User {
  id: number
  nome: string
  email: string
  setor: UserSector
  cargo: UserRole
}

export interface Ticket {
  id: number
  titulo: string
  descricao: string | null
  status: TicketStatus
  usuario: User
}

export interface PageMetadata {
  size: number
  totalElements: number
  totalPages: number
  number: number
}

export interface Page<T> {
  content: T[]
  page: PageMetadata
}

export interface TicketFilters {
  status?: TicketStatus
  solicitanteId?: number
  page?: number
  size?: number
  sort?: string
}

export interface CreateTicketInput {
  titulo: string
  descricao?: string | null
  usuarioId: number
}

export interface UpdateTicketInput {
  titulo: string
  descricao?: string | null
  status: TicketStatus
}

export interface CreateUserInput {
  nome: string
  email: string
  senha: string
  setor: UserSector
}

export interface UpdateUserInput {
  nome: string
  email: string
  setor: UserSector
}

export interface ValidationError {
  campo: string
  mensagem: string
}
