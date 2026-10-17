import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  async function handleSubmit(event) {
    event.preventDefault(); setError(''); setLoading(true)
    try {
      const result = await authService.login(form)
      const user = result.user || { name: result.name || form.email.split('@')[0], email: form.email, role: result.role || 'USER' }
      signIn(user, result.token || result.accessToken)
      navigate(location.state?.from || '/dashboard')
    } catch (err) {
      setError(`${err.message}. Check that Spring Boot is running and the /auth/login API exists.`)
    } finally { setLoading(false) }
  }

  return <section className="auth-layout"><div className="auth-aside"><span className="auth-symbol">♡</span><p className="eyebrow">WELCOME BACK</p><h1>Care feels better when we're connected.</h1><p>Sign in to manage your care requests and keep your family support in one place.</p></div>
    <form className="form-card" onSubmit={handleSubmit}><p className="eyebrow">YOUR CARECONNECT ACCOUNT</p><h2>Log in</h2><p className="muted">Welcome back. Please enter your details.</p>
      {error && <div className="alert alert-error">{error}</div>}
      <label>Email address<input type="email" required autoComplete="email" placeholder="you@example.com" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
      <label>Password<input type="password" required autoComplete="current-password" placeholder="Enter your password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /></label>
      <button className="btn btn-primary full-width" disabled={loading}>{loading ? 'Signing in…' : 'Log in securely'}</button>
      <p className="form-foot">New to CareConnect? <Link to="/register">Create an account</Link></p>
    </form>
  </section>
}
