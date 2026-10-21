import type {
  CreateTicketInput,
  CreateUserInput,
  Page,
  Ticket,
  TicketFilters,
  UpdateTicketInput,
  UpdateUserInput,
  User,
  ValidationError,
} from '../types'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL?.trim() || '/api').replace(
  /\/+$/,
  '',
)

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: ValidationError[]

  constructor(message: string, status: number, fieldErrors: ValidationError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Confira os dados informados e tente novamente.'
    case 401:
      return 'É necessário entrar na sua conta para continuar.'
    case 403:
      return 'Você não tem permissão para realizar esta ação.'
    case 404:
      return 'O registro solicitado não foi encontrado.'
    case 409:
      return 'Esta ação entra em conflito com o estado atual do registro.'
    case 422:
      return 'Não foi possível processar os dados informados.'
    case 429:
      return 'Muitas solicitações foram enviadas. Aguarde um momento e tente novamente.'
    default:
      return status >= 500
        ? 'O servidor não conseguiu concluir a solicitação. Tente novamente em instantes.'
        : `Não foi possível concluir a solicitação (HTTP ${status}).`
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isValidationError(value: unknown): value is ValidationError {
  return (
    isRecord(value) &&
    typeof value.campo === 'string' &&
    typeof value.mensagem === 'string'
  )
}

function readMessage(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined
  const message = value.trim()
  return message.length > 0 ? message : undefined
}

function responseError(status: number, body: unknown): ApiError {
  if (status >= 500) return new ApiError(fallbackMessage(status), status)

  if (Array.isArray(body)) {
    const fieldErrors = body.filter(isValidationError)
    if (fieldErrors.length > 0) {
      return new ApiError('Confira os campos destacados.', status, fieldErrors)
    }
  }

  // O backend usa mensagens públicas no ProblemDetail ou no formato de erro do Spring Boot.
  if (isRecord(body)) {
    const message =
      readMessage(body.detail) ??
      readMessage(body.message) ??
      readMessage(body.title)

    if (message) return new ApiError(message, status)
  }

  return new ApiError(fallbackMessage(status), status)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  try {
    const headers = new Headers(options.headers)
    headers.set('Accept', 'application/json')
    if (options.body !== undefined) headers.set('Content-Type', 'application/json')

    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    })
    const text = await response.text()
    let body: unknown

    // Criação e exclusão podem retornar sucesso sem um corpo JSON.
    if (text.trim()) {
      try {
        body = JSON.parse(text)
      } catch {
        if (response.ok) {
          throw new ApiError(
            'O servidor devolveu uma resposta em formato inesperado.',
            response.status,
          )
        }
      }
    }

    if (!response.ok) throw responseError(response.status, body)
    return body as T
  } catch (error) {
    if (error instanceof ApiError || options.signal?.aborted) throw error
    if (error instanceof Error && error.name === 'AbortError') throw error

    throw new ApiError(
      'Não foi possível conectar à API. Confira se o backend está em execução e tente novamente.',
      0,
    )
  }
}

export function listTickets(
  filters: TicketFilters = {},
  signal?: AbortSignal,
): Promise<Page<Ticket>> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.solicitanteId !== undefined) {
    params.set('solicitanteId', String(filters.solicitanteId))
  }
  if (filters.page !== undefined) params.set('page', String(filters.page))
  if (filters.size !== undefined) params.set('size', String(filters.size))
  if (filters.sort) params.set('sort', filters.sort)

  const query = params.toString()
  return request<Page<Ticket>>(`/chamados${query ? `?${query}` : ''}`, { signal })
}

export function listUsers(signal?: AbortSignal): Promise<User[]> {
  return request<User[]>('/usuarios', { signal })
}

export function createTicket(input: CreateTicketInput): Promise<void> {
  return request<void>('/chamados', { method: 'POST', body: JSON.stringify(input) })
}

export function updateTicket(id: number, input: UpdateTicketInput): Promise<Ticket> {
  return request<Ticket>(`/chamados/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

export function deleteTicket(id: number): Promise<void> {
  return request<void>(`/chamados/${id}`, { method: 'DELETE' })
}

export function createUser(input: CreateUserInput): Promise<void> {
  return request<void>('/usuarios', { method: 'POST', body: JSON.stringify(input) })
}

export function updateUser(id: number, input: UpdateUserInput): Promise<User> {
  return request<User>(`/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(input) })
}

export function deleteUser(id: number): Promise<void> {
  return request<void>(`/usuarios/${id}`, { method: 'DELETE' })
}
