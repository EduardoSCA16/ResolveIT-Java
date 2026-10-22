import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { AnimatePresence, motion, MotionConfig } from 'motion/react'
import {
  ArrowDownUp, ArrowRight, ArrowUpRight, Check, CheckCheck, ChevronLeft,
  ChevronRight, CircleDot, CircleHelp, Clock3, Headphones, LayoutDashboard,
  LayoutGrid, List, LogOut, Plus, RefreshCw, Search, SlidersHorizontal,
  Ticket as TicketIcon, Users, WifiOff, X,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import RecordDrawer, { type DrawerState } from './components/RecordDrawer'
import AuthScreen from './components/AuthScreen'
import { ApiError, currentUser, listTickets, listUsers, logout } from './lib/api'
import {
  PROFILE_LABELS, SECTOR_LABELS, STATUS_LABELS, TICKET_STATUSES, USER_SECTORS,
  type Page, type Ticket, type TicketStatus, type User,
} from './types'

type View = 'overview' | 'tickets' | 'users'
type Counts = Record<TicketStatus, number>
const NAV: { view: View; hash: string; label: string; icon: LucideIcon }[] = [
  { view: 'overview', hash: 'visao-geral', label: 'Visão geral', icon: LayoutDashboard },
  { view: 'tickets', hash: 'chamados', label: 'Chamados', icon: TicketIcon },
  { view: 'users', hash: 'pessoas', label: 'Pessoas', icon: Users },
]
const STATUS_INFO: Record<TicketStatus, { plural: string; hint: string; icon: LucideIcon }> = {
  ABERTO: { plural: 'Abertos', hint: 'Prontos para o primeiro passo', icon: CircleDot },
  EM_ANDAMENTO: { plural: 'Em andamento', hint: 'Soluções sendo construídas', icon: Clock3 },
  CONCLUIDO: { plural: 'Concluídos', hint: 'Atendimento realizado', icon: Check },
  FECHADO: { plural: 'Fechados', hint: 'Ciclo encerrado', icon: CheckCheck },
}

function readView(): View {
  return NAV.find((item) => `#${item.hash}` === window.location.hash)?.view ?? 'overview'
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Não foi possível carregar os dados.'
}

function Logo({ compact = false }: { compact?: boolean }) {
  return <span className={`brand${compact ? ' brand-compact' : ''}`}>
    <span className="brand-symbol" aria-hidden="true"><Check strokeWidth={3} /></span>
    <span className="brand-word">resolve<span>it</span><i /></span>
  </span>
}

function StatusBadge({ status }: { status: TicketStatus }) {
  return <span className={`status-badge status-${status}`}><span className="status-dot" />{STATUS_LABELS[status]}</span>
}

function PersonAvatar({ user, small = false }: { user: User; small?: boolean }) {
  return <span className={`person-avatar sector-${user.setor}${small ? ' avatar-small' : ''}`} aria-hidden="true">{initials(user.nome)}</span>
}

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  useEffect(() => {
    const controller = new AbortController()
    currentUser(controller.signal).then(setUser).catch((error: unknown) => {
      if (!controller.signal.aborted && !(error instanceof ApiError && error.status === 401)) setNotice(errorMessage(error))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    const expired = () => { setUser(null); setNotice('Sua sessão expirou. Entre novamente.') }
    window.addEventListener('resolveit:unauthorized', expired)
    return () => { controller.abort(); window.removeEventListener('resolveit:unauthorized', expired) }
  }, [])
  if (loading) return <main className="auth-loading" role="status">Carregando sua sessão…</main>
  if (!user) return <AuthScreen notice={notice} onLogin={(value) => { setNotice(''); setUser(value) }} />
  return <Workspace key={`${user.id}-${user.perfil}`} user={user} onLogout={async () => {
    await logout()
    setUser(null)
    setNotice('Você saiu da sua conta.')
  }} />
}

function Workspace({ user, onLogout }: { user: User; onLogout: () => Promise<void> }) {
  const isAdmin = user.perfil === 'ADMIN'
  const navigation = NAV.filter((item) => isAdmin || item.view !== 'users')
  const permittedView = () => !isAdmin && readView() === 'users' ? 'overview' : readView()
  const [view, setView] = useState<View>(permittedView)
  const [signingOut, setSigningOut] = useState(false)
  const [drawer, setDrawer] = useState<DrawerState>(null)
  const [helpOpen, setHelpOpen] = useState(false)
  const [toast, setToast] = useState('')
  const [reload, setReload] = useState(0)
  const [users, setUsers] = useState<User[]>([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [usersError, setUsersError] = useState('')
  const [tickets, setTickets] = useState<Page<Ticket> | null>(null)
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState('')
  const [counts, setCounts] = useState<Counts | null>(null)
  const [countsLoading, setCountsLoading] = useState(true)
  const [status, setStatus] = useState<TicketStatus | ''>('')
  const [requester, setRequester] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(8)
  const [sort, setSort] = useState('id,desc')
  const [mode, setMode] = useState<'list' | 'grid'>('list')
  const [userSearch, setUserSearch] = useState('')
  const [lastSync, setLastSync] = useState<Date | null>(null)

  useEffect(() => {
    const changed = () => { setView(!isAdmin && readView() === 'users' ? 'overview' : readView()); window.scrollTo({ top: 0 }) }
    window.addEventListener('hashchange', changed)
    return () => window.removeEventListener('hashchange', changed)
  }, [isAdmin])

  useEffect(() => {
    document.title = `${NAV.find((item) => item.view === view)?.label} · ResolveIT`
  }, [view])

  useEffect(() => {
    const controller = new AbortController()
    if (!isAdmin) {
      setUsers([user])
      setUsersLoading(false)
      return () => controller.abort()
    }
    setUsersLoading(true)
    setUsersError('')
    listUsers(controller.signal)
      .then((data) => {
        setUsers(data)
        setRequester((current) => current && !data.some((user) => String(user.id) === current) ? '' : current)
        setLastSync(new Date())
      })
      .catch((error: unknown) => { if (!controller.signal.aborted) setUsersError(errorMessage(error)) })
      .finally(() => { if (!controller.signal.aborted) setUsersLoading(false) })
    return () => controller.abort()
  }, [reload, isAdmin, user])

  useEffect(() => {
    const controller = new AbortController()
    setCountsLoading(true)
    Promise.all(TICKET_STATUSES.map(async (value) => {
      const response = await listTickets({ status: value, page: 0, size: 1 }, controller.signal)
      return [value, response.page.totalElements] as const
    }))
      .then((entries) => { setCounts(Object.fromEntries(entries) as Counts); setLastSync(new Date()) })
      .catch(() => { if (!controller.signal.aborted) setCounts(null) })
      .finally(() => { if (!controller.signal.aborted) setCountsLoading(false) })
    return () => controller.abort()
  }, [reload])

  useEffect(() => {
    if (view === 'users') return
    const controller = new AbortController()
    setTicketsLoading(true)
    setTicketsError('')
    const overview = view === 'overview'
    listTickets({
      status: overview ? undefined : status || undefined,
      solicitanteId: overview || !requester ? undefined : Number(requester),
      page: overview ? 0 : page,
      size: overview ? 5 : size,
      sort: overview ? 'id,desc' : sort,
    }, controller.signal)
      .then((data) => {
        if (!overview && page > 0 && page >= data.page.totalPages) {
          setPage(Math.max(0, data.page.totalPages - 1))
          return
        }
        setTickets(data)
        setLastSync(new Date())
      })
      .catch((error: unknown) => { if (!controller.signal.aborted) setTicketsError(errorMessage(error)) })
      .finally(() => { if (!controller.signal.aborted) setTicketsLoading(false) })
    return () => controller.abort()
  }, [view, status, requester, page, size, sort, reload])

  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(''), 5000)
    return () => window.clearTimeout(timeout)
  }, [toast])

  useEffect(() => {
    const shortcut = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      if (event.ctrlKey || event.metaKey || event.altKey || target?.isContentEditable
        || ['INPUT', 'SELECT', 'TEXTAREA'].includes(target?.tagName ?? '') || drawer || helpOpen) return
      if (event.key.toLowerCase() === 'n') {
        event.preventDefault()
        setDrawer({ kind: view === 'users' ? 'user' : 'ticket' })
      }
    }
    window.addEventListener('keydown', shortcut)
    return () => window.removeEventListener('keydown', shortcut)
  }, [drawer, helpOpen, view])

  function navigate(next: View) {
    if (!isAdmin && next === 'users') return
    window.location.hash = NAV.find((item) => item.view === next)!.hash
  }

  function filterStatus(next: TicketStatus | '') {
    setStatus(next)
    setPage(0)
    navigate('tickets')
  }

  function resetFilters() {
    setStatus('')
    setRequester('')
    setPage(0)
  }

  function saved(message: string) {
    setToast(message)
    setReload((value) => value + 1)
  }

  async function signOut() {
    setSigningOut(true)
    try { await onLogout() } catch (error) { setToast(errorMessage(error)) }
    finally { setSigningOut(false) }
  }

  const total = counts ? Object.values(counts).reduce((sum, count) => sum + count, 0) : null
  const resolved = counts ? counts.CONCLUIDO + counts.FECHADO : 0
  const percentage = total ? Math.round(resolved / total * 100) : 0
  const connection = usersLoading && !lastSync ? 'loading' : usersError && !counts ? 'error' : lastSync ? 'online' : 'loading'
  const visibleUsers = useMemo(() => {
    const query = userSearch.trim().toLocaleLowerCase('pt-BR')
    return users.filter((user) => `${user.nome} ${user.email} ${SECTOR_LABELS[user.setor]}`.toLocaleLowerCase('pt-BR').includes(query))
  }, [userSearch, users])
  const currentNav = NAV.find((item) => item.view === view)!

  return (
    <MotionConfig reducedMotion="user">
      <a className="skip-link" href="#main-content" onClick={(event) => { event.preventDefault(); document.getElementById('main-content')?.focus() }}>Pular para o conteúdo</a>
      <div className="app-shell">
        <aside className="sidebar" aria-label="Menu principal">
          <a className="brand-link" href="#visao-geral" aria-label="ResolveIT, visão geral"><Logo /></a>
          <div className="workspace-switch"><span className="workspace-icon"><Headphones size={16} /></span><div><strong>Central de suporte</strong><span>Seu espaço de trabalho</span></div><span className="workspace-dot" /></div>
          <span className="nav-caption">WORKSPACE</span>
          <nav className="side-navigation">
            {navigation.map(({ view: target, hash, label, icon: Icon }) => <a key={target} href={`#${hash}`} className={`nav-item ${view === target ? 'active' : ''}`} aria-current={view === target ? 'page' : undefined} title={label}>
              <Icon size={19} /><span>{label}</span>{target === 'tickets' && total !== null && <small>{total}</small>}
            </a>)}
          </nav>
          <div className="sidebar-tip"><span className="tip-spark">✳</span><strong>Um passo de cada vez.</strong><p>Grandes soluções começam com um chamado.</p><button onClick={() => setDrawer({ kind: 'ticket' })}>Vamos resolver <ArrowUpRight size={15} /></button><span className="tip-orbit" /></div>
          <div className="sidebar-bottom"><button className="nav-item" onClick={() => setHelpOpen(true)} title="Como funciona"><CircleHelp size={19} /><span>Como funciona</span><ArrowUpRight size={14} /></button><div className="workspace-footer"><span className="footer-mark">RI</span><div><strong>{user.nome}</strong><span>{PROFILE_LABELS[user.perfil]}</span></div></div></div>
        </aside>

        <div className="main-shell">
          <header className="topbar">
            <a className="mobile-brand" href="#visao-geral" aria-label="ResolveIT, início"><Logo compact /></a>
            <div className="breadcrumb"><span>Workspace</span><ChevronRight size={13} /><strong>{currentNav.label}</strong></div>
            <div className="topbar-actions"><span className={`connection connection-${connection}`}><i />{connection === 'online' ? 'API conectada' : connection === 'error' ? 'Sem conexão' : 'Conectando'}</span><span className="topbar-separator" /><button className="icon-button" onClick={() => setHelpOpen(true)} title="Como funciona" aria-label="Como funciona"><CircleHelp size={19} /></button><button className="button button-ghost logout-button" onClick={() => void signOut()} disabled={signingOut}><LogOut size={17} />{signingOut ? 'Saindo…' : 'Sair'}</button></div>
          </header>

          <main className="main-content" id="main-content" tabIndex={-1}>
            <AnimatePresence mode="wait">
              <motion.div key={view} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: .22 }}>
                {view === 'overview' ? <>
                  <div className="page-heading"><div><p className="eyebrow">TUDO NO SEU LUGAR</p><h1>Visão geral<span className="heading-dot">.</span></h1><p>Uma visão clara para transformar pedidos em soluções.</p></div><button className="button button-ghost refresh-button" onClick={() => setReload((value) => value + 1)} disabled={ticketsLoading || usersLoading}><RefreshCw size={15} className={ticketsLoading || usersLoading ? 'spin' : ''} />Atualizar</button></div>
                  <section className="hero-panel" aria-labelledby="hero-title">
                    <div className="hero-copy"><span className="hero-label"><span />MENOS RUÍDO. MAIS SOLUÇÕES.</span><h2 id="hero-title">O próximo passo<br />começa <em>aqui.</em><span className="hero-asterisk">✳</span></h2><p>Conecte pessoas, organize demandas e dê<br className="desktop-break" /> espaço para o que realmente importa: resolver.</p><button className="button button-primary" onClick={() => setDrawer({ kind: 'ticket' })}><Plus size={18} />Novo chamado<kbd>N</kbd></button></div>
                    <SignalArtwork />
                    <span className="hero-bottom-label">PESSOAS CONECTADAS. TRABALHO EM MOVIMENTO.</span>
                  </section>
                </> : <div className="page-heading"><div><p className="eyebrow">{view === 'tickets' ? 'DO PRIMEIRO PEDIDO À SOLUÇÃO' : 'QUEM FAZ PARTE DA SOLUÇÃO'}</p><h1>{view === 'tickets' ? 'Central de chamados' : 'Pessoas & equipes'}<span className="heading-dot">.</span></h1><p>{view === 'tickets' ? 'Acompanhe cada demanda, sem perder o próximo passo.' : 'Gente conectada, atendimento mais próximo.'}</p></div><button className="button button-primary" onClick={() => setDrawer({ kind: view === 'users' ? 'user' : 'ticket' })}><Plus size={18} />{view === 'users' ? 'Nova pessoa' : 'Novo chamado'}<kbd>N</kbd></button></div>}

                {view !== 'users' && <section className="metrics-grid" aria-label="Chamados por status">
                  {TICKET_STATUSES.map((value, index) => {
                    const { icon: Icon, plural, hint } = STATUS_INFO[value]
                    return <button key={value} className={`metric-card ${index === 0 ? 'metric-featured' : ''} ${view === 'tickets' && status === value ? 'metric-selected' : ''}`} onClick={() => filterStatus(value)} aria-label={`Filtrar chamados ${plural.toLocaleLowerCase('pt-BR')}`}>
                      <div className="metric-top"><span>{plural}</span><Icon size={18} /></div><div className="metric-value">{countsLoading ? <span className="skeleton skeleton-number" /> : counts ? String(counts[value]).padStart(2, '0') : '—'}<ArrowUpRight size={18} /></div><span className="metric-hint">{hint}</span>
                    </button>
                  })}
                </section>}

                {view === 'overview' && <div className="overview-grid">
                  <section className="panel recent-panel"><div className="panel-heading"><div><span className="eyebrow">ACOMPANHE O MOVIMENTO</span><h2>Últimos chamados <span className="subtle-count">{total ?? '—'}</span></h2></div><button className="text-button" onClick={() => navigate('tickets')}>Ver todos<ArrowUpRight size={15} /></button></div><TicketContent loading={ticketsLoading} error={ticketsError} tickets={tickets?.content ?? []} mode="list" onOpen={(ticket) => setDrawer({ kind: 'ticket', ticket })} onRetry={() => setReload((value) => value + 1)} onNew={() => setDrawer({ kind: 'ticket' })} /><div className="panel-footnote"><span className="tiny-dot" />Cada chamado é um próximo passo.{lastSync && <span>Atualizado às {lastSync.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</span>}</div></section>
                  <aside className="overview-side"><section className="panel progress-panel"><div className="panel-heading"><div><span className="eyebrow">DA DEMANDA À ENTREGA</span><h2>O fluxo acontece.</h2></div><ArrowUpRight size={18} className="muted" /></div><div className="resolution-ring" style={{ '--progress': `${percentage}%` } as CSSProperties}><div><strong>{counts ? `${percentage}%` : '—'}</strong><span>concluídos ou fechados</span></div></div><div className="flow-legend"><span><i className="legend-lime" />Soluções entregues<strong>{counts ? resolved : '—'}</strong></span><span><i className="legend-dark" />Em aberto ou andamento<strong>{counts ? counts.ABERTO + counts.EM_ANDAMENTO : '—'}</strong></span></div><p className="panel-description">{total === 0 ? 'Sua próxima solução começa com o primeiro chamado.' : 'Uma etapa de cada vez, até a solução.'}</p></section>{isAdmin && <section className="small-callout"><span className="callout-icon"><Users size={19} /></span><div><strong>Pessoas por trás dos pedidos</strong><p>{usersError ? 'Conheça e organize seus solicitantes.' : `${users.length} ${users.length === 1 ? 'pessoa conectada' : 'pessoas conectadas'} à central.`}</p></div><button className="icon-button" aria-label="Ver pessoas" onClick={() => navigate('users')}><ArrowUpRight size={19} /></button></section>}</aside>
                </div>}

                {view === 'tickets' && <section className="panel tickets-panel" aria-label="Lista de chamados">
                  <div className="filter-toolbar"><div className="filter-title"><SlidersHorizontal size={17} /><span>Encontre seu próximo passo</span></div><button className="text-button muted" onClick={resetFilters} disabled={!status && !requester}>Limpar filtros<X size={13} /></button></div>
                  <div className="filter-controls"><label><span>Status</span><select value={status} onChange={(event) => { setStatus(event.target.value as TicketStatus | ''); setPage(0) }}><option value="">Todos os status</option>{TICKET_STATUSES.map((value) => <option key={value} value={value}>{STATUS_LABELS[value]}</option>)}</select></label>{isAdmin && <label className="requester-filter"><span>Solicitante</span><select value={requester} disabled={usersLoading || Boolean(usersError)} onChange={(event) => { setRequester(event.target.value); setPage(0) }}><option value="">{usersError ? 'Pessoas indisponíveis' : 'Todas as pessoas'}</option>{users.map((user) => <option key={user.id} value={user.id}>{user.nome}</option>)}</select></label>}<label className="sort-filter"><span><ArrowDownUp size={12} />Ordenar</span><select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0) }}><option value="id,desc">Mais recentes</option><option value="id,asc">Mais antigos</option><option value="titulo,asc">Título: A–Z</option></select></label></div>
                  {usersError && <p className="inline-notice">O filtro por pessoa está indisponível no momento. <button className="text-button" onClick={() => setReload((value) => value + 1)}>Tentar novamente</button></p>}
                  <div className="list-heading"><div><h2>{status ? `Chamados ${STATUS_INFO[status].plural.toLocaleLowerCase('pt-BR')}` : (isAdmin ? 'Todos os chamados' : 'Meus chamados')}</h2><span className="subtle-count">{ticketsLoading || ticketsError ? '…' : tickets?.page.totalElements ?? 0}</span></div><div className="view-toggle" aria-label="Modo de visualização"><button className={mode === 'list' ? 'active' : ''} onClick={() => setMode('list')} aria-label="Visualizar como lista" aria-pressed={mode === 'list'}><List size={16} /></button><button className={mode === 'grid' ? 'active' : ''} onClick={() => setMode('grid')} aria-label="Visualizar como cartões" aria-pressed={mode === 'grid'}><LayoutGrid size={16} /></button></div></div>
                  <TicketContent loading={ticketsLoading} error={ticketsError} tickets={tickets?.content ?? []} mode={mode} onOpen={(ticket) => setDrawer({ kind: 'ticket', ticket })} onRetry={() => setReload((value) => value + 1)} onNew={() => status || requester ? resetFilters() : setDrawer({ kind: 'ticket' })} filtered={Boolean(status || requester)} />
                  <div className="pagination"><label><span>Por página</span><select value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0) }}><option value={8}>8</option><option value={16}>16</option><option value={32}>32</option></select></label><span className="pagination-summary">{ticketsError ? 'Dados indisponíveis' : ticketsLoading ? 'Carregando…' : tickets?.page.totalElements ? `${page * size + 1}–${Math.min((page + 1) * size, tickets.page.totalElements)} de ${tickets.page.totalElements}` : 'Nenhum chamado'}</span><div className="page-controls"><button className="icon-button" aria-label="Página anterior" disabled={ticketsLoading || Boolean(ticketsError) || page === 0} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={17} /></button><span>{page + 1}<span> / {Math.max(1, tickets?.page.totalPages ?? 1)}</span></span><button className="icon-button" aria-label="Próxima página" disabled={ticketsLoading || Boolean(ticketsError) || page + 1 >= (tickets?.page.totalPages ?? 0)} onClick={() => setPage((value) => value + 1)}><ChevronRight size={17} /></button></div></div>
                </section>}

                {view === 'users' && <>
                  <section className="people-banner"><div><span className="eyebrow">CONECTAR PARA RESOLVER</span><h2>O suporte começa<br />pelas <em>pessoas.</em></h2><p>Uma central mais próxima de quem precisa.</p></div><div className="people-totals"><div><strong>{usersError ? '—' : usersLoading ? '…' : String(users.length).padStart(2, '0')}</strong><span>pessoas na central</span></div><span /><div><strong>{usersError ? '—' : usersLoading ? '…' : String(new Set(users.map((user) => user.setor)).size).padStart(2, '0')}</strong><span>setores conectados</span></div></div><div className="people-banner-art" aria-hidden="true"><Users size={100} strokeWidth={.8} /></div></section>
                  <section className="people-section"><div className="people-toolbar"><div><h2>Quem está por aqui <span className="subtle-count">{visibleUsers.length}</span></h2><p>Encontre pessoas por nome, e-mail ou setor.</p></div><label className="search-field"><Search size={17} /><span className="sr-only">Buscar pessoas por nome, e-mail ou setor</span><input type="search" placeholder="Buscar uma pessoa…" value={userSearch} onChange={(event) => setUserSearch(event.target.value)} /></label></div>
                  {usersLoading ? <div className="people-grid">{Array.from({ length: 6 }, (_, index) => <div className="person-card skeleton-card" key={index}><span className="skeleton skeleton-avatar" /><span className="skeleton skeleton-line" /><span className="skeleton skeleton-line short" /></div>)}</div> : usersError ? <div className="panel"><ErrorState message={usersError} onRetry={() => setReload((value) => value + 1)} /></div> : <div className="people-grid">{visibleUsers.map((user) => <motion.button layout key={user.id} className="person-card" onClick={() => setDrawer({ kind: 'user', user })}><div className="person-card-top"><PersonAvatar user={user} /><span className="role-label">{PROFILE_LABELS[user.perfil]}</span><ArrowUpRight size={16} /></div><h3>{user.nome}</h3><p className="person-email">{user.email}</p><div className="person-card-footer"><span><span className="tiny-dot" />{SECTOR_LABELS[user.setor]}</span><span>#{String(user.id).padStart(3, '0')}</span></div></motion.button>)}{!userSearch && <button className="person-card add-person-card" onClick={() => setDrawer({ kind: 'user' })}><span className="add-person-icon"><Plus size={24} /></span><strong>Gente nova por aqui?</strong><p>Conecte uma pessoa à central.</p><span className="text-button">Cadastrar pessoa<ArrowRight size={15} /></span></button>}{userSearch && visibleUsers.length === 0 && <div className="empty-search"><Search size={26} /><h3>Nenhuma pessoa encontrada</h3><p>Tente outro nome, e-mail ou setor.</p><button className="text-button" onClick={() => setUserSearch('')}>Limpar busca</button></div>}</div>}</section>
                  {!usersError && !usersLoading && users.length > 0 && <section className="sectors-strip"><span className="eyebrow">CONEXÕES POR SETOR</span><div>{USER_SECTORS.filter((sector) => users.some((user) => user.setor === sector)).map((sector) => <span key={sector}>{SECTOR_LABELS[sector]}<strong>{users.filter((user) => user.setor === sector).length}</strong></span>)}</div></section>}
                </>}
                <footer className="page-footer"><span>resolve<span>it</span><i /> <span className="footer-phrase">Menos ruído. Mais soluções.</span></span><span>Feito para conectar e resolver <span className="footer-spark">✳</span></span></footer>
              </motion.div>
            </AnimatePresence>
          </main>
        </div>

        <nav className="mobile-navigation" aria-label="Menu principal móvel">{navigation.map(({ view: target, hash, label, icon: Icon }) => <a key={target} href={`#${hash}`} className={view === target ? 'active' : ''} aria-current={view === target ? 'page' : undefined}><Icon size={20} /><span>{label}</span></a>)}</nav>
        <RecordDrawer state={drawer} users={users} currentUser={user} onClose={() => setDrawer(null)} onSaved={saved} />
        <HelpDialog open={helpOpen} onClose={() => setHelpOpen(false)} onCreate={() => { setHelpOpen(false); setDrawer({ kind: 'ticket' }) }} />
        <AnimatePresence>{toast && <motion.div className="toast" role="status" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}><span><Check size={17} /></span><p>{toast}</p><button className="icon-button" onClick={() => setToast('')} aria-label="Dispensar mensagem"><X size={16} /></button></motion.div>}</AnimatePresence>
      </div>
    </MotionConfig>
  )
}

function SignalArtwork() {
  return <div className="signal-art" aria-hidden="true"><div className="signal-glow" /><svg viewBox="0 0 400 300" className="signal-lines"><defs><linearGradient id="signal-gradient" x1="0" y1="1" x2="1" y2="0"><stop stopColor="#386d4e" /><stop offset="1" stopColor="#bdff82" /></linearGradient></defs><ellipse cx="210" cy="155" rx="150" ry="70" transform="rotate(-32 210 155)" /><ellipse cx="210" cy="155" rx="130" ry="90" transform="rotate(-32 210 155)" /><ellipse cx="210" cy="155" rx="110" ry="110" transform="rotate(-32 210 155)" /><path d="M65 255 345 56" /><circle cx="210" cy="155" r="38" className="signal-center" /><path d="m193 156 12 12 25-28" className="signal-check" /><circle cx="90" cy="225" r="8" className="signal-point" /><circle cx="329" cy="85" r="6" className="signal-point-secondary" /></svg><div className="signal-chip signal-chip-top"><span className="tiny-dot" />Um novo jeito de resolver<ArrowUpRight size={14} /></div><div className="signal-chip signal-chip-bottom"><span className="signal-mini-check"><Check size={15} /></span><div><strong>Conecte. Acompanhe. Resolva.</strong><span>O trabalho segue em frente.</span></div></div><span className="signal-coordinate">R / 01 — EM MOVIMENTO</span></div>
}

function TicketContent({ loading, error, tickets, mode, onOpen, onRetry, onNew, filtered = false }: {
  loading: boolean; error: string; tickets: Ticket[]; mode: 'list' | 'grid';
  onOpen: (ticket: Ticket) => void; onRetry: () => void; onNew: () => void; filtered?: boolean;
}) {
  if (loading) return <div className="ticket-skeletons" role="status" aria-label="Carregando chamados">{Array.from({ length: 4 }, (_, index) => <div className="ticket-skeleton" key={index}><span className="skeleton skeleton-avatar" /><div><span className="skeleton skeleton-line" /><span className="skeleton skeleton-line short" /></div><span className="skeleton skeleton-pill" /></div>)}</div>
  if (error) return <ErrorState message={error} onRetry={onRetry} />
  if (tickets.length === 0) return <div className="empty-state"><span className="empty-icon"><TicketIcon size={30} strokeWidth={1.3} /></span><span className="eyebrow">ESPAÇO PARA O PRÓXIMO PASSO</span><h3>{filtered ? 'Nenhum chamado por aqui.' : 'Toda solução tem um começo.'}</h3><p>{filtered ? 'Experimente outra combinação de status e solicitante.' : 'Abra seu primeiro chamado e coloque o atendimento em movimento.'}</p><button className="button button-ghost" onClick={onNew}>{filtered ? 'Limpar filtros' : 'Criar primeiro chamado'}<ArrowUpRight size={15} /></button></div>
  return <>
    {mode === 'list' && <div className="ticket-table-wrap"><table className="ticket-table"><thead><tr><th>Chamado</th><th>Solicitante</th><th>Status</th><th><span className="sr-only">Ações</span></th></tr></thead><tbody>{tickets.map((ticket) => <tr key={ticket.id} className="ticket-row" onClick={() => onOpen(ticket)}><td><button className="ticket-title-button" onClick={(event) => { event.stopPropagation(); onOpen(ticket) }}><span className="ticket-number">#{String(ticket.id).padStart(3, '0')}</span><strong>{ticket.titulo}</strong><span className="ticket-description">{ticket.descricao || 'Sem descrição adicional.'}</span></button></td><td><div className="requester-cell"><PersonAvatar user={ticket.usuario} small /><div><strong>{ticket.usuario.nome}</strong><span>{SECTOR_LABELS[ticket.usuario.setor]}</span></div></div></td><td><StatusBadge status={ticket.status} /></td><td><button className="icon-button row-open" aria-label={`Abrir chamado ${ticket.id}: ${ticket.titulo}`} onClick={(event) => { event.stopPropagation(); onOpen(ticket) }}><ArrowUpRight size={18} /></button></td></tr>)}</tbody></table></div>}
    <div className={`ticket-card-grid ${mode === 'list' ? 'mobile-ticket-cards' : ''}`}>{tickets.map((ticket) => <motion.button layout className="ticket-card" key={ticket.id} onClick={() => onOpen(ticket)}><div className="ticket-card-top"><span className="ticket-number">#{String(ticket.id).padStart(3, '0')}</span><StatusBadge status={ticket.status} /></div><h3>{ticket.titulo}</h3><p>{ticket.descricao || 'Sem descrição adicional.'}</p><div className="ticket-card-bottom"><PersonAvatar user={ticket.usuario} small /><div><strong>{ticket.usuario.nome}</strong><span>{SECTOR_LABELS[ticket.usuario.setor]}</span></div><ArrowUpRight size={18} /></div></motion.button>)}</div>
  </>
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div className="empty-state error-state" role="alert"><span className="empty-icon"><WifiOff size={28} strokeWidth={1.4} /></span><span className="eyebrow">VAMOS RECONECTAR</span><h3>Uma pausa na conexão.</h3><p>{message}</p><button className="button button-ghost" onClick={onRetry}><RefreshCw size={15} />Tentar novamente</button></div>
}

function HelpDialog({ open, onClose, onCreate }: { open: boolean; onClose: () => void; onCreate: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = ref.current
    if (!dialog || !open) return
    const previousFocus = document.activeElement
    dialog.showModal()
    return () => { dialog.close(); if (previousFocus instanceof HTMLElement) previousFocus.focus() }
  }, [open])
  return <dialog ref={ref} className="help-dialog" aria-labelledby="help-title" onCancel={(event) => { event.preventDefault(); onClose() }} onClick={(event) => { if (event.target === event.currentTarget) { const bounds = event.currentTarget.getBoundingClientRect(); if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose() } }}><button className="icon-button help-close" onClick={onClose} aria-label="Fechar ajuda"><X size={20} /></button><span className="help-symbol">✳</span><p className="eyebrow">SIMPLES PARA COMEÇAR</p><h2 id="help-title">Do pedido à solução.</h2><p className="help-intro">Um lugar para conectar quem precisa de ajuda a um próximo passo.</p><ol className="help-steps"><li><span>01</span><div><h3>Conecte as pessoas</h3><p>Crie sua conta para pedir suporte. Administradores gerenciam as contas na área Pessoas.</p></div></li><li><span>02</span><div><h3>Abra espaço para resolver</h3><p>Crie um chamado com título e detalhes. Ele ficará vinculado à sua conta.</p></div></li><li><span>03</span><div><h3>Acompanhe o próximo passo</h3><p>Usuários acompanham os próprios chamados. Administradores podem escolher qualquer status, inclusive concluir, fechar ou reabrir diretamente.</p></div></li></ol><button className="button button-primary" onClick={onCreate}>Criar um chamado<ArrowRight size={17} /></button></dialog>
}
