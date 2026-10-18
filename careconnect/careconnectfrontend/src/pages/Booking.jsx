import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { apiRequest } from '../services/api'
import { useAuth } from '../context/AuthContext'

const PAYMENT_METHODS = [
  { id: 'CARD', label: 'Credit / Debit Card', icon: '💳', desc: 'Visa, Mastercard, RuPay' },
  { id: 'UPI', label: 'UPI / QR Code', icon: '📱', desc: 'Google Pay, PhonePe, Paytm' },
  { id: 'NET_BANKING', label: 'Net Banking', icon: '🏦', desc: 'All major banks' },
  { id: 'CASH', label: 'Cash on Service', icon: '💵', desc: 'Pay directly after care session' }
]

export default function Booking() {
  const [params] = useSearchParams()
  const { user } = useAuth()
  const [caregivers, setCaregivers] = useState([])
  const [form, setForm] = useState({
    caregiverId: params.get('caregiverId') || '',
    careType: (params.get('type') || 'BABY').toUpperCase().includes('ELDER') ? 'ELDER' : 'BABY',
    bookingDate: '',
    hours: '4',
    notes: '',
    paymentMethod: 'CARD'
  })
  const [message, setMessage] = useState('')
  const [bookingSummary, setBookingSummary] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    apiRequest('/caregivers')
      .then(data => { if (Array.isArray(data)) setCaregivers(data) })
      .catch(() => {})
  }, [])

  const selectedCaregiver = caregivers.find(cg => String(cg.id) === String(form.caregiverId))
  const hourlyRate = selectedCaregiver?.hourly_rate ? Number(selectedCaregiver.hourly_rate) : 100.00
  const hours = Math.max(1, Number(form.hours) || 1)
  const totalAmount = (hourlyRate * hours).toFixed(2)

  async function submit(e) {
    e.preventDefault()
    setMessage('')
    setError('')
    setBookingSummary(null)
    setLoading(true)

    try {
      const res = await apiRequest('/bookings', {
        method: 'POST',
        body: JSON.stringify({
          caregiverId: form.caregiverId ? Number(form.caregiverId) : null,
          userId: user?.id,
          careType: form.careType,
          bookingDate: form.bookingDate,
          hours: hours,
          notes: form.notes,
          paymentMethod: form.paymentMethod
        })
      })

      const confirmedAmount = res.totalAmount != null ? Number(res.totalAmount).toFixed(2) : totalAmount
      setBookingSummary({
        bookingId: res.id,
        careType: form.careType === 'ELDER' ? 'Elder Care' : 'Baby Care',
        caregiverName: selectedCaregiver?.name || 'Assigned automatically',
        date: form.bookingDate,
        hours: hours,
        hourlyRate: hourlyRate.toFixed(2),
        totalAmount: confirmedAmount,
        paymentMethod: res.paymentMethod || form.paymentMethod,
        paymentStatus: res.paymentStatus || 'UNPAID'
      })

      setMessage('Booking request submitted successfully!')
      setForm(f => ({ ...f, bookingDate: '', notes: '' }))
    } catch (err) {
      setError(err.message || 'Failed to submit booking request.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="form-page">
      <div className="booking-page-container">
        <form className="form-card booking-form" onSubmit={submit}>
          <p className="eyebrow">CARE REQUEST & BILLING</p>
          <h1>Book Care</h1>
          <p className="muted">Specify your care requirements, check the real-time amount, and pick a payment method.</p>

          {message && <div className="alert alert-success">{message}</div>}
          {error && <div className="alert alert-error">{error}</div>}

          {bookingSummary && (
            <div className="booking-success-card">
              <div className="success-header">
                <span className="success-icon">✓</span>
                <div>
                  <strong>Booking Confirmed! (Ref #{bookingSummary.bookingId})</strong>
                  <p className="muted">Your caregiver request and invoice have been registered.</p>
                </div>
              </div>
              <div className="summary-details-grid">
                <div><span>Service:</span> <strong>{bookingSummary.careType}</strong></div>
                <div><span>Caregiver:</span> <strong>{bookingSummary.caregiverName}</strong></div>
                <div><span>Hours:</span> <strong>{bookingSummary.hours} hrs @ ₹{bookingSummary.hourlyRate}/hr</strong></div>
                <div><span>Total Amount:</span> <strong className="highlight-amount">₹{bookingSummary.totalAmount}</strong></div>
                <div><span>Payment Method:</span> <strong>{PAYMENT_METHODS.find(m => m.id === bookingSummary.paymentMethod)?.label || bookingSummary.paymentMethod}</strong></div>
                <div><span>Invoice Status:</span> <span className="pill pill-amber">{bookingSummary.paymentStatus}</span></div>
              </div>
              <div className="summary-actions">
                <Link to="/payments" className="btn btn-primary btn-small">Go to Payments & Invoices →</Link>
                <Link to="/dashboard" className="btn btn-outline btn-small">Dashboard</Link>
              </div>
            </div>
          )}

          <label>
            Care Type
            <select value={form.careType} onChange={e => setForm({ ...form, careType: e.target.value })}>
              <option value="BABY">Baby Care (Infant & Toddler)</option>
              <option value="ELDER">Elder Care (Senior Support)</option>
            </select>
          </label>

          <label>
            Select Caregiver
            <select value={form.caregiverId} onChange={e => setForm({ ...form, caregiverId: e.target.value })}>
              <option value="">Auto-assign best available caregiver (₹100.00/hr)</option>
              {caregivers.map(cg => (
                <option key={cg.id} value={cg.id}>
                  {cg.name || `Caregiver #${cg.id}`} — ₹{cg.hourly_rate ? Number(cg.hourly_rate).toFixed(2) : '100.00'}/hr {cg.skills ? `(${cg.skills})` : ''}
                </option>
              ))}
            </select>
          </label>

          <label>
            Preferred Date
            <input
              type="date"
              required
              min={new Date().toISOString().slice(0, 10)}
              value={form.bookingDate}
              onChange={e => setForm({ ...form, bookingDate: e.target.value })}
            />
          </label>

          <label>
            Estimated Duration (Hours)
            <input
              type="number"
              min="1"
              max="24"
              value={form.hours}
              onChange={e => setForm({ ...form, hours: e.target.value })}
            />
          </label>

          {/* PAYMENT METHOD SELECTION */}
          <div className="form-group-block">
            <span className="field-title">Choose Payment Method</span>
            <p className="field-hint">Select how you prefer to pay for this care session:</p>
            <div className="payment-methods-grid">
              {PAYMENT_METHODS.map(m => (
                <label
                  key={m.id}
                  className={`payment-method-card ${form.paymentMethod === m.id ? 'active' : ''}`}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={m.id}
                    checked={form.paymentMethod === m.id}
                    onChange={e => setForm({ ...form, paymentMethod: e.target.value })}
                  />
                  <span className="method-icon">{m.icon}</span>
                  <div className="method-text">
                    <strong>{m.label}</strong>
                    <small>{m.desc}</small>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <label>
            Additional Details & Special Instructions
            <textarea
              rows="3"
              placeholder="Share routine details, medications, dietary needs, or preferences…"
              value={form.notes}
              onChange={e => setForm({ ...form, notes: e.target.value })}
            />
          </label>

          {/* DYNAMIC AMOUNT CALCULATION SUMMARY */}
          <div className="amount-calculation-card">
            <div className="calc-header">
              <span className="calc-badge">TOTAL BILLING AMOUNT</span>
              <span className="calc-sub">Transparent pricing with no hidden fees</span>
            </div>
            <div className="calc-rows">
              <div className="calc-row">
                <span>Caregiver Rate:</span>
                <strong>₹{hourlyRate.toFixed(2)} / hr</strong>
              </div>
              <div className="calc-row">
                <span>Duration:</span>
                <strong>{hours} {hours === 1 ? 'hour' : 'hours'}</strong>
              </div>
              <div className="calc-row">
                <span>Payment Method:</span>
                <strong>{PAYMENT_METHODS.find(m => m.id === form.paymentMethod)?.label}</strong>
              </div>
              <div className="calc-divider"></div>
              <div className="calc-row total-highlight-row">
                <div>
                  <strong>Total Amount for Booking:</strong>
                  <small>Guaranteed care & verified coverage</small>
                </div>
                <span className="calc-total-val">₹{totalAmount}</span>
              </div>
            </div>
          </div>

          <button className="btn btn-primary full-width submit-booking-btn" disabled={loading}>
            {loading ? 'Submitting request…' : `Confirm & Book for ₹${totalAmount}`}
          </button>
        </form>
      </div>
    </section>
  )
}
