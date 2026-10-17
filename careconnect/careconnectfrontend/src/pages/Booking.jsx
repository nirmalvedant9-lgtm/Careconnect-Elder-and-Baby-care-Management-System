import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiRequest } from '../services/api'
import { useAuth } from '../context/AuthContext'

export default function Booking() {
  const [params] = useSearchParams()
  const { user } = useAuth()
  const [caregivers, setCaregivers] = useState([])
  const [form, setForm] = useState({
    caregiverId: params.get('caregiverId') || '',
    careType: (params.get('type') || 'BABY').toUpperCase().includes('ELDER') ? 'ELDER' : 'BABY',
    bookingDate: '',
    hours: '4',
    notes: ''
  })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    apiRequest('/caregivers')
      .then(data => { if (Array.isArray(data)) setCaregivers(data) })
      .catch(() => {})
  }, [])

  async function submit(e) {
    e.preventDefault(); setMessage(''); setError(''); setLoading(true)
    try {
      await apiRequest('/bookings', {
        method: 'POST',
        body: JSON.stringify({
          caregiverId: form.caregiverId ? Number(form.caregiverId) : null,
          userId: user?.id,
          careType: form.careType,
          bookingDate: form.bookingDate,
          hours: Number(form.hours) || 4,
          notes: form.notes
        })
      })
      setMessage('Booking request sent successfully!')
      setForm(f => ({ ...f, bookingDate: '', notes: '' }))
    } catch (err) {
      setError(err.message || 'Failed to submit booking request.')
    } finally { setLoading(false) }
  }

  return <section className="form-page"><form className="form-card booking-form" onSubmit={submit}><p className="eyebrow">CARE REQUEST</p><h1>Book care</h1><p className="muted">Share a few details so your request can be recorded.</p>
    {message && <div className="alert alert-success">{message}</div>}{error && <div className="alert alert-error">{error}</div>}
    <label>Care type<select value={form.careType} onChange={e => setForm({ ...form, careType: e.target.value })}><option value="BABY">Baby care</option><option value="ELDER">Elder care</option></select></label>
    <label>Caregiver<select value={form.caregiverId} onChange={e => setForm({ ...form, caregiverId: e.target.value })}>
      <option value="">Auto-assign best available caregiver</option>
      {caregivers.map(cg => (
        <option key={cg.id} value={cg.id}>{cg.name || `Caregiver #${cg.id}`} {cg.skills ? `(${cg.skills})` : ''}</option>
      ))}
    </select></label>
    <label>Preferred date<input type="date" required min={new Date().toISOString().slice(0, 10)} value={form.bookingDate} onChange={e => setForm({ ...form, bookingDate: e.target.value })} /></label>
    <label>Estimated hours<input type="number" min="1" max="24" value={form.hours} onChange={e => setForm({ ...form, hours: e.target.value })} /></label>
    <label>Additional details<textarea rows="4" placeholder="Tell us about the support you need…" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></label>
    <button className="btn btn-primary full-width" disabled={loading}>{loading ? 'Sending request…' : 'Submit booking request'}</button>
  </form></section>
}
