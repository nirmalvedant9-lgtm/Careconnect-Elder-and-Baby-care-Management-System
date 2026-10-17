import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'

export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'FAMILY' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  async function handleSubmit(event) {
    event.preventDefault(); setError(''); setSuccess(''); setLoading(true)
    try {
      await authService.register(form)
      setSuccess('Account created successfully. You can now log in.')
      setTimeout(() => navigate('/login'), 900)
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.')
    } finally { setLoading(false) }
  }
  return <section className="auth-layout"><div className="auth-aside"><span className="auth-symbol">♡</span><p className="eyebrow">JOIN OUR COMMUNITY</p><h1>A little more support. A lot more peace of mind.</h1><p>Create your account to explore caregiver options and manage bookings.</p></div>
    <form className="form-card" onSubmit={handleSubmit}><p className="eyebrow">GET STARTED</p><h2>Create account</h2><p className="muted">It only takes a few details.</p>
      {error && <div className="alert alert-error">{error}</div>}{success && <div className="alert alert-success">{success}</div>}
      <label>Full name<input required placeholder="Your full name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
      <label>Email address<input type="email" required placeholder="you@example.com" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
      <label>Password<input type="password" required minLength="6" placeholder="At least 6 characters" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} /></label>
      <label>Account type<select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}><option value="FAMILY">Family (Elder & Baby Care)</option><option value="CAREGIVER">Caregiver</option></select></label>
      <button className="btn btn-primary full-width" disabled={loading}>{loading ? 'Creating account…' : 'Create account'}</button>
      <p className="form-foot">Already registered? <Link to="/login">Log in</Link></p>
    </form>
  </section>
}
