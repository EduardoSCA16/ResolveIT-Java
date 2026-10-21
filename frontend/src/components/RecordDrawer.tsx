import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { AlertCircle, ArrowRight, Check, LoaderCircle, Trash2, X } from 'lucide-react'
import {
  ApiError,
  createTicket,
  createUser,
  deleteTicket,
  deleteUser,
  updateTicket,
  updateUser,
} from '../lib/api'
import {
  SECTOR_LABELS,
  STATUS_LABELS,
  USER_SECTORS,
  type Ticket,
  type TicketStatus,
  type User,
} from '../types'

export type DrawerState =
  | { kind: 'ticket'; ticket?: Ticket }
  | { kind: 'user'; user?: User }
  | null

type Props = {
  state: DrawerState
  users: User[]
  onClose: () => void
  onSaved: (message: string) => void
}

const NEXT_STATUS: Partial<Record<TicketStatus, TicketStatus>> = {
  ABERTO: 'EM_ANDAMENTO',
  EM_ANDAMENTO: 'CONCLUIDO',
  CONCLUIDO: 'FECHADO',
}

export default function RecordDrawer({ state, users, onClose, onSaved }: Props) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [pending, setPending] = useState(false)
  const identity = state?.kind === 'ticket'
    ? `ticket-${state.ticket?.id ?? 'new'}`
    : state?.kind === 'user' ? `user-${state.user?.id ?? 'new'}` : null

  useEffect(() => {
    const element = dialog.current
    if (!element || !identity) return

    const previousFocus = document.activeElement
    if (!element.open) element.showModal()

    return () => {
      element.close()
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus()
      }
    }
  }, [identity])

  return (
    <dialog
      ref={dialog}
      className="record-drawer"
      aria-labelledby="drawer-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!pending) onClose()
      }}
      onClick={(event) => {
        if (event.target !== event.currentTarget || pending) return
        const bounds = event.currentTarget.getBoundingClientRect()
        if (event.clientX < bounds.left || event.clientX > bounds.right
          || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose()
      }}
    >
      {state && (
        <RecordForm
          key={identity}
          state={state}
          users={users}
          onClose={onClose}
          onPending={setPending}
          onSaved={(message) => {
            onSaved(message)
            onClose()
          }}
        />
      )}
    </dialog>
  )
}

function RecordForm({ state, users, onClose, onSaved, onPending }: {
  state: NonNullable<DrawerState>
  users: User[]
  onClose: () => void
  onSaved: (message: string) => void
  onPending: (value: boolean) => void
}) {
  const ticket = state.kind === 'ticket' ? state.ticket : undefined
  const user = state.kind === 'user' ? state.user : undefined
  const isTicket = state.kind === 'ticket'
  const editing = Boolean(ticket || user)
  const closed = ticket?.status === 'FECHADO'
  const [titulo, setTitulo] = useState(ticket?.titulo ?? '')
  const [descricao, setDescricao] = useState(ticket?.descricao ?? '')
  const [status, setStatus] = useState<TicketStatus>(ticket?.status ?? 'ABERTO')
  const [usuarioId, setUsuarioId] = useState('')
  const [nome, setNome] = useState(user?.nome ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [senha, setSenha] = useState('')
  const [setor, setSetor] = useState<User['setor']>(user?.setor ?? USER_SECTORS[0])
  const [pending, setPending] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const requestInFlight = useRef(false)
  const errorBox = useRef<HTMLDivElement>(null)
  const title = isTicket
    ? ticket ? `Chamado #${String(ticket.id).padStart(3, '0')}` : 'Novo chamado'
    : user ? 'Editar usuário' : 'Novo usuário'

  useEffect(() => {
    if (error) errorBox.current?.focus()
  }, [error])

  function showError(cause: unknown) {
    if (cause instanceof ApiError) {
      setError(cause.message)
      setFieldErrors(Object.fromEntries(
        (cause.fieldErrors ?? []).map((item) => [item.campo, item.mensagem]),
      ))
    } else {
      setError(cause instanceof Error ? cause.message : 'Não foi possível concluir a operação.')
    }
  }

  async function mutate(action: () => Promise<unknown>, success: string) {
    if (requestInFlight.current) return
    requestInFlight.current = true
    setPending(true)
    onPending(true)
    setError('')
    setFieldErrors({})
    try {
      await action()
      onSaved(success)
    } catch (cause) {
      showError(cause)
    } finally {
      requestInFlight.current = false
      setPending(false)
      onPending(false)
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (closed || pending || confirmDelete) return

    const invalid: Record<string, string> = {}
    if (isTicket) {
      if (!titulo.trim()) invalid.titulo = 'Informe o título.'
      if (!editing && !usuarioId) invalid.usuarioId = 'Selecione o solicitante.'
    } else {
      if (!nome.trim()) invalid.nome = 'Informe o nome.'
      if (!email.trim()) invalid.email = 'Informe o e-mail.'
      if (!editing && !senha.trim()) invalid.senha = 'Informe a senha.'
    }
    if (Object.keys(invalid).length) {
      setFieldErrors(invalid)
      setError('Confira os campos indicados antes de continuar.')
      return
    }

    if (isTicket) {
      const data = { titulo: titulo.trim(), descricao: descricao.trim() }
      if (ticket) {
        void mutate(() => updateTicket(ticket.id, { ...data, status }), 'Chamado atualizado.')
      } else {
        void mutate(() => createTicket({ ...data, usuarioId: Number(usuarioId) }), 'Chamado criado. Vamos resolver!')
      }
    } else {
      const data = { nome: nome.trim(), email: email.trim(), setor }
      if (user) {
        void mutate(() => updateUser(user.id, data), 'Usuário atualizado.')
      } else {
        void mutate(() => createUser({ ...data, senha }), 'Usuário cadastrado.')
      }
    }
  }

  function remove() {
    if (!confirmDelete) return
    if (ticket) void mutate(() => deleteTicket(ticket.id), 'Chamado excluído.')
    if (user) void mutate(() => deleteUser(user.id), 'Usuário excluído.')
  }

  const allowedStatuses = ticket
    ? [ticket.status, NEXT_STATUS[ticket.status]].filter((value): value is TicketStatus => Boolean(value))
    : []
  const noUsers = isTicket && !editing && users.length === 0

  return (
    <form className="drawer-form" onSubmit={submit} aria-busy={pending}>
      <header className="drawer-header">
        <div>
          <p className="eyebrow">{isTicket ? 'CENTRAL DE CHAMADOS' : 'PESSOAS & EQUIPES'}</p>
          <h2 id="drawer-title">{title}</h2>
          <p className="drawer-subtitle">
            {isTicket ? editing ? 'Acompanhe os detalhes e o próximo passo.' : 'Conte o que aconteceu. O próximo passo começa aqui.'
              : editing ? 'Mantenha os dados de contato atualizados.' : 'Uma nova pessoa para fazer parte da solução.'}
          </p>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar painel" onClick={onClose} disabled={pending}>
          <X size={20} aria-hidden="true" />
        </button>
      </header>

      <div className="drawer-body">
        {error && (
          <div className="form-error" role="alert" tabIndex={-1} ref={errorBox}>
            <AlertCircle size={18} aria-hidden="true" />
            <div>
              <strong>Não foi possível concluir</strong>
              <p>{error}</p>
            </div>
          </div>
        )}

        {isTicket ? (
          <>
            <div className="drawer-state-line">
              <span className={`status-badge status-${ticket?.status ?? 'ABERTO'}`}>
                <span className="status-dot" />{STATUS_LABELS[ticket?.status ?? 'ABERTO']}
              </span>
              <span>{ticket ? `Solicitante: ${ticket.usuario.nome}` : 'Todo novo chamado começa aberto.'}</span>
            </div>
            {closed && <p className="form-notice">Este chamado foi encerrado. Seus dados estão disponíveis para consulta.</p>}
            <Field id="record-title" label="Título" error={fieldErrors.titulo}>
              <input id="record-title" value={titulo} onChange={(event) => setTitulo(event.target.value)} placeholder="Ex.: não consigo acessar o sistema" required readOnly={closed} disabled={pending} aria-invalid={Boolean(fieldErrors.titulo)} aria-describedby={fieldErrors.titulo ? 'record-title-error' : undefined} />
            </Field>
            <Field id="record-description" label="Descrição" hint="Opcional. Inclua detalhes que ajudem no atendimento." error={fieldErrors.descricao}>
              <textarea id="record-description" value={descricao} onChange={(event) => setDescricao(event.target.value)} placeholder="O que aconteceu? Quando começou?" rows={5} readOnly={closed} disabled={pending} aria-invalid={Boolean(fieldErrors.descricao)} aria-describedby={fieldErrors.descricao ? 'record-description-error' : 'record-description-hint'} />
            </Field>
            {ticket ? (
              <Field id="record-status" label="Status do atendimento" hint={closed ? 'Chamados fechados não podem ser alterados.' : 'Avance uma etapa ou mantenha o status atual.'} error={fieldErrors.status}>
                <select id="record-status" value={status} onChange={(event) => setStatus(event.target.value as TicketStatus)} disabled={closed || pending} aria-describedby={fieldErrors.status ? 'record-status-error' : 'record-status-hint'}>
                  {allowedStatuses.map((value) => <option key={value} value={value}>{STATUS_LABELS[value]}</option>)}
                </select>
              </Field>
            ) : (
              <Field id="record-requester" label="Solicitante" error={fieldErrors.usuarioId}>
                <select id="record-requester" value={usuarioId} onChange={(event) => setUsuarioId(event.target.value)} required disabled={pending || noUsers} aria-invalid={Boolean(fieldErrors.usuarioId)} aria-describedby={fieldErrors.usuarioId ? 'record-requester-error' : undefined}>
                  <option value="">Selecione uma pessoa</option>
                  {users.map((person) => <option key={person.id} value={person.id}>{person.nome} · {SECTOR_LABELS[person.setor]}</option>)}
                </select>
              </Field>
            )}
            {noUsers && <p className="form-notice">Cadastre um usuário na área Pessoas antes de abrir o primeiro chamado.</p>}
          </>
        ) : (
          <>
            {user && <div className="drawer-state-line"><span className="person-avatar">{user.nome.slice(0, 1).toUpperCase()}</span><span>Usuário #{user.id} · {user.cargo === 'TECNICO' ? 'Técnico' : 'Solicitante'}</span></div>}
            <Field id="record-name" label="Nome completo" error={fieldErrors.nome}>
              <input id="record-name" autoComplete="name" value={nome} onChange={(event) => setNome(event.target.value)} placeholder="Como a pessoa se chama?" required disabled={pending} aria-invalid={Boolean(fieldErrors.nome)} aria-describedby={fieldErrors.nome ? 'record-name-error' : undefined} />
            </Field>
            <Field id="record-email" label="E-mail" error={fieldErrors.email}>
              <input id="record-email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="pessoa@empresa.com" required disabled={pending} aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? 'record-email-error' : undefined} />
            </Field>
            <Field id="record-sector" label="Setor" error={fieldErrors.setor}>
              <select id="record-sector" value={setor} onChange={(event) => setSetor(event.target.value as User['setor'])} required disabled={pending}>
                {USER_SECTORS.map((value) => <option key={value} value={value}>{SECTOR_LABELS[value]}</option>)}
              </select>
            </Field>
            {!editing && (
              <Field id="record-password" label="Senha" hint="Usada para o cadastro. Ela não será exibida nas consultas." error={fieldErrors.senha}>
                <input id="record-password" type="password" autoComplete="new-password" value={senha} onChange={(event) => setSenha(event.target.value)} required minLength={1} disabled={pending} aria-invalid={Boolean(fieldErrors.senha)} aria-describedby={fieldErrors.senha ? 'record-password-error' : 'record-password-hint'} />
              </Field>
            )}
            {!editing && <p className="form-notice">Novos usuários são cadastrados como solicitantes.</p>}
          </>
        )}

        {editing && (
          <section className="drawer-danger" aria-label="Excluir registro">
            {confirmDelete ? (
              <div className="delete-confirmation">
                <strong>Excluir {ticket ? `o chamado #${ticket.id}` : user?.nome}?</strong>
                <p>Esta ação é permanente.{user ? ' Usuários com chamados vinculados não podem ser excluídos.' : ''}</p>
                <div className="button-row">
                  <button className="button button-danger" type="button" onClick={remove} disabled={pending}>
                    {pending ? <LoaderCircle size={16} className="spin" aria-hidden="true" /> : <Trash2 size={16} aria-hidden="true" />} Confirmar exclusão
                  </button>
                  <button className="button button-ghost" type="button" onClick={() => setConfirmDelete(false)} disabled={pending}>Manter registro</button>
                </div>
              </div>
            ) : (
              <button className="text-button text-danger" type="button" onClick={() => setConfirmDelete(true)} disabled={pending}><Trash2 size={16} aria-hidden="true" />Excluir {isTicket ? 'chamado' : 'usuário'}</button>
            )}
          </section>
        )}
      </div>

      <footer className="drawer-footer">
        <button className="button button-ghost" type="button" onClick={onClose} disabled={pending}>{closed ? 'Fechar' : 'Cancelar'}</button>
        {!closed && !confirmDelete && <button className="button button-primary" type="submit" disabled={pending || noUsers}>
          {pending ? <LoaderCircle size={17} className="spin" aria-hidden="true" /> : editing ? <Check size={17} aria-hidden="true" /> : <ArrowRight size={17} aria-hidden="true" />}
          {pending ? 'Salvando…' : editing ? 'Salvar alterações' : isTicket ? 'Criar chamado' : 'Cadastrar usuário'}
        </button>}
      </footer>
    </form>
  )
}

function Field({ id, label, hint, error, children }: {
  id: string
  label: string
  hint?: string
  error?: string
  children: ReactNode
}) {
  return (
    <div className={`form-field${error ? ' has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error ? <p className="field-error" id={`${id}-error`}>{error}</p>
        : hint ? <p className="field-hint" id={`${id}-hint`}>{hint}</p> : null}
    </div>
  )
}
