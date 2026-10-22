import { useRef, useState, type FormEvent } from 'react'
import { ArrowRight, Check, LoaderCircle, LockKeyhole } from 'lucide-react'
import { ApiError, login, register } from '../lib/api'
import { SECTOR_LABELS, USER_SECTORS, type User, type UserSector } from '../types'

export default function AuthScreen({ onLogin, notice = '' }: { onLogin: (user: User) => void; notice?: string }) {
  const [signup, setSignup] = useState(false)
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [setor, setSetor] = useState<UserSector>('TI')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState(notice)
  const inFlight = useRef(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (inFlight.current) return
    inFlight.current = true
    setPending(true)
    setError('')
    setMessage('')
    try {
      if (signup) {
        await register({ nome: nome.trim(), email: email.trim(), senha, setor })
        setSignup(false)
        setSenha('')
        setMessage('Conta criada! Entre com seu e-mail e sua senha.')
      } else {
        onLogin(await login(email.trim(), senha))
      }
    } catch (cause) {
      setError(cause instanceof ApiError && cause.fieldErrors.length
        ? cause.fieldErrors.map((field) => field.mensagem).join(' ')
        : cause instanceof Error ? cause.message : 'Não foi possível continuar.')
    } finally {
      inFlight.current = false
      setPending(false)
    }
  }

  return <main className="auth-page">
    <section className="auth-story" aria-label="ResolveIT">
      <a className="brand" href="#"><span className="brand-symbol"><Check strokeWidth={3} /></span><span className="brand-word">resolve<span>it</span><i /></span></a>
      <div><p className="eyebrow">SEU SUPORTE COMEÇA AQUI</p><h1>Um pedido.<br />Um próximo <em>passo.</em></h1><p>Abra seus chamados, acompanhe o atendimento e encontre suas soluções em um só lugar.</p></div>
      <span className="auth-story-note"><LockKeyhole size={18} />Seu espaço, seus chamados.</span>
    </section>
    <section className="auth-panel" aria-labelledby="auth-title">
      <p className="eyebrow">BEM-VINDO AO RESOLVEIT</p>
      <h2 id="auth-title">{signup ? 'Crie sua conta.' : 'Bom ter você aqui.'}</h2>
      <p className="auth-intro">{signup ? 'Preencha seus dados para começar a pedir suporte.' : 'Entre para acompanhar o que precisa ser resolvido.'}</p>
      <form onSubmit={submit} aria-busy={pending}>
        {error && <p className="form-error" role="alert">{error}</p>}
        {message && <p className="form-notice" role="status">{message}</p>}
        {signup && <div className="form-field"><label htmlFor="auth-name">Nome completo</label><input id="auth-name" autoComplete="name" required value={nome} onChange={(event) => setNome(event.target.value)} disabled={pending} /></div>}
        <div className="form-field"><label htmlFor="auth-email">E-mail</label><input id="auth-email" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} disabled={pending} placeholder="voce@empresa.com" /></div>
        <div className="form-field"><label htmlFor="auth-password">Senha</label><input id="auth-password" type="password" autoComplete={signup ? 'new-password' : 'current-password'} required minLength={signup ? 8 : undefined} maxLength={signup ? 64 : undefined} value={senha} onChange={(event) => setSenha(event.target.value)} disabled={pending} aria-describedby={signup ? 'password-help' : undefined} />{signup && <p className="field-hint" id="password-help">Use de 8 a 64 caracteres.</p>}</div>
        {signup && <div className="form-field"><label htmlFor="auth-sector">Setor</label><select id="auth-sector" value={setor} onChange={(event) => setSetor(event.target.value as UserSector)} disabled={pending}>{USER_SECTORS.map((item) => <option key={item} value={item}>{SECTOR_LABELS[item]}</option>)}</select></div>}
        <button className="button button-primary auth-submit" disabled={pending}>{pending ? <LoaderCircle className="spin" size={18} /> : <ArrowRight size={18} />}{pending ? 'Aguarde…' : signup ? 'Criar conta' : 'Entrar'}</button>
      </form>
      <p className="auth-switch">{signup ? 'Já tem uma conta?' : 'Primeira vez por aqui?'} <button className="text-button" disabled={pending} onClick={() => { setSignup(!signup); setError(''); setMessage(''); setSenha('') }}>{signup ? 'Entrar' : 'Criar conta'}</button></p>
    </section>
  </main>
}
