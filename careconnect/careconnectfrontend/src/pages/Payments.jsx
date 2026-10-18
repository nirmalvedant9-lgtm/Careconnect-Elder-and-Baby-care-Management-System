import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import { apiRequest } from '../services/api'
import { useAuth } from '../context/AuthContext'

const PAYMENT_METHODS = [
  { id: 'CARD', label: 'Credit / Debit Card', icon: '💳' },
  { id: 'UPI', label: 'UPI / QR Code', icon: '📱' },
  { id: 'NET_BANKING', label: 'Net Banking', icon: '🏦' },
  { id: 'CASH', label: 'Cash on Service', icon: '💵' }
]

export default function Payments() {
  const { user } = useAuth()
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL')
  const [activePaymentModal, setActivePaymentModal] = useState(null)
  const [selectedMethod, setSelectedMethod] = useState('CARD')
  const [paying, setPaying] = useState(false)
  const [actionMessage, setActionMessage] = useState('')
  const [actionError, setActionError] = useState('')

  function loadPayments() {
    setLoading(true)
    apiRequest('/payments')
      .then(data => {
        if (Array.isArray(data)) setPayments(data)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadPayments()
  }, [])

  async function handlePay(e) {
    e.preventDefault()
    if (!activePaymentModal) return
    setPaying(true)
    setActionMessage('')
    setActionError('')

    try {
      const res = await apiRequest(`/payments/${activePaymentModal.id}/pay`, {
        method: 'PUT',
        body: JSON.stringify({ paymentMethod: selectedMethod })
      })

      setActionMessage(res.message || 'Payment completed successfully!')
      setActivePaymentModal(null)
      loadPayments()
    } catch (err) {
      setActionError(err.message || 'Payment processing failed. Please try again.')
    } finally {
      setPaying(false)
    }
  }

  const filtered = payments.filter(p => {
    if (filter === 'PAID') return p.status === 'PAID'
    if (filter === 'UNPAID') return p.status !== 'PAID'
    return true
  })

  const totalInvoiced = payments.reduce((acc, p) => acc + (Number(p.amount) || 0), 0)
  const totalPaid = payments
    .filter(p => p.status === 'PAID')
    .reduce((acc, p) => acc + (Number(p.amount) || 0), 0)
  const totalPending = totalInvoiced - totalPaid

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <section className="dashboard-content">
        <p className="eyebrow">BILLING & INVOICES</p>
        <h1>Payments & Amount Overview</h1>
        <p className="muted">Manage your booking invoices, review amounts, and pay using your preferred payment method.</p>

        {actionMessage && <div className="alert alert-success" style={{ marginTop: '1rem' }}>{actionMessage}</div>}
        {actionError && <div className="alert alert-error" style={{ marginTop: '1rem' }}>{actionError}</div>}

        {/* PAYMENT METRICS STRIP */}
        <div className="dashboard-stats" style={{ margin: '1.8rem 0' }}>
          <article>
            <span className="stat-icon mint">💳</span>
            <small>Total Invoiced</small>
            <strong>₹{totalInvoiced.toFixed(2)}</strong>
            <span className="muted">{payments.length} booking invoices</span>
          </article>
          <article>
            <span className="stat-icon peach">✓</span>
            <small>Total Paid</small>
            <strong style={{ color: '#2e7d32' }}>₹{totalPaid.toFixed(2)}</strong>
            <span className="muted">Settled payments</span>
          </article>
          <article>
            <span className="stat-icon lavender">⏳</span>
            <small>Pending Amount</small>
            <strong style={{ color: totalPending > 0 ? '#b45309' : '#173052' }}>₹{totalPending.toFixed(2)}</strong>
            <span className="muted">Awaiting settlement</span>
          </article>
        </div>

        {/* INVOICE FILTER BAR */}
        <div className="filter-row" style={{ margin: '1.5rem 0 1rem' }}>
          <label>
            Filter Status:
            <select value={filter} onChange={e => setFilter(e.target.value)}>
              <option value="ALL">All Invoices ({payments.length})</option>
              <option value="UNPAID">Pending / Unpaid</option>
              <option value="PAID">Completed / Paid</option>
            </select>
          </label>
          <Link to="/booking" className="btn btn-primary btn-small">＋ New Booking</Link>
        </div>

        {/* INVOICE LIST */}
        {loading ? (
          <div className="dashboard-panel" style={{ textAlign: 'center', padding: '3rem' }}>
            <p className="muted">Loading invoices and payment methods…</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="dashboard-panel" style={{ textAlign: 'center', padding: '3rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.8rem' }}>🧾</div>
            <h2>No invoices found</h2>
            <p className="muted" style={{ marginBottom: '1.5rem' }}>
              {filter === 'ALL'
                ? "You haven't made any care bookings yet. Book a caregiver to generate an invoice."
                : `No ${filter.toLowerCase()} invoices at this time.`}
            </p>
            <Link to="/booking" className="btn btn-primary">Book Care Now</Link>
          </div>
        ) : (
          <div className="payments-list">
            {filtered.map(item => {
              const methodObj = PAYMENT_METHODS.find(m => m.id === item.payment_method) || {
                label: item.payment_method || 'Card',
                icon: '💳'
              }
              const isPaid = item.status === 'PAID'
              const dateStr = item.service_date ? new Date(item.service_date).toLocaleDateString() : 'Scheduled date'
              const amountVal = item.amount != null ? Number(item.amount).toFixed(2) : '0.00'

              return (
                <div key={item.id} className="payment-invoice-card">
                  <div className="invoice-left">
                    <div className="invoice-meta-top">
                      <span className="invoice-ref">{item.transaction_id || `INV-CC-${10000 + item.id}`}</span>
                      <span className={`pill ${isPaid ? 'pill-green' : 'pill-amber'}`}>
                        {isPaid ? '✓ Paid' : '⏳ Pending Payment'}
                      </span>
                    </div>
                    <h3>{item.care_type === 'ELDER' ? 'Elder Care Service' : 'Baby Care Service'}</h3>
                    <div className="invoice-details">
                      <span>👤 Caregiver: <strong>{item.caregiver_name || 'Care Professional'}</strong></span>
                      <span>📅 Date: <strong>{dateStr}</strong></span>
                      <span>⏱️ Duration: <strong>{item.hours || 4} hours</strong></span>
                      <span>For: <strong>{item.dependent_name || 'Loved One'}</strong></span>
                    </div>
                  </div>

                  <div className="invoice-right">
                    <div className="invoice-method-tag">
                      <span>{methodObj.icon}</span>
                      <span>{methodObj.label}</span>
                    </div>
                    <div className="invoice-amount-block">
                      <small>Total Amount</small>
                      <strong className="invoice-amount">₹{amountVal}</strong>
                    </div>

                    {isPaid ? (
                      <div className="paid-stamp">
                        <span>✓ Settled</span>
                      </div>
                    ) : (
                      <button
                        className="btn btn-primary btn-small pay-now-btn"
                        onClick={() => {
                          setActivePaymentModal(item)
                          setSelectedMethod(item.payment_method || 'CARD')
                        }}
                      >
                        Pay ₹{amountVal} Now →
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* PAY NOW POPUP MODAL */}
        {activePaymentModal && (
          <div className="payment-modal-backdrop" onClick={() => !paying && setActivePaymentModal(null)}>
            <div className="payment-modal-card" onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <div>
                  <p className="eyebrow">COMPLETE PAYMENT</p>
                  <h2>Pay Invoice #{activePaymentModal.transaction_id || `INV-CC-${10000 + activePaymentModal.id}`}</h2>
                </div>
                <button
                  type="button"
                  className="close-modal-btn"
                  disabled={paying}
                  onClick={() => setActivePaymentModal(null)}
                >
                  ✕
                </button>
              </div>

              <div className="modal-summary-box">
                <div className="modal-summary-row">
                  <span>Care Service:</span>
                  <strong>{activePaymentModal.care_type === 'ELDER' ? 'Elder Care' : 'Baby Care'}</strong>
                </div>
                <div className="modal-summary-row">
                  <span>Caregiver:</span>
                  <strong>{activePaymentModal.caregiver_name}</strong>
                </div>
                <div className="modal-summary-row">
                  <span>Duration:</span>
                  <strong>{activePaymentModal.hours} hours</strong>
                </div>
                <div className="modal-divider"></div>
                <div className="modal-summary-row total">
                  <span>Amount to Pay:</span>
                  <strong className="modal-total-price">₹{Number(activePaymentModal.amount || 0).toFixed(2)}</strong>
                </div>
              </div>

              <form onSubmit={handlePay}>
                <span className="field-title" style={{ display: 'block', marginTop: '1.2rem' }}>
                  Select Payment Method:
                </span>
                <div className="payment-methods-grid" style={{ margin: '0.8rem 0 1.5rem' }}>
                  {PAYMENT_METHODS.map(m => (
                    <label
                      key={m.id}
                      className={`payment-method-card ${selectedMethod === m.id ? 'active' : ''}`}
                    >
                      <input
                        type="radio"
                        name="modalPaymentMethod"
                        value={m.id}
                        checked={selectedMethod === m.id}
                        onChange={e => setSelectedMethod(e.target.value)}
                      />
                      <span className="method-icon">{m.icon}</span>
                      <div className="method-text">
                        <strong>{m.label}</strong>
                      </div>
                    </label>
                  ))}
                </div>

                <div className="modal-actions">
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={paying}
                    onClick={() => setActivePaymentModal(null)}
                  >
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={paying}>
                    {paying ? 'Processing Payment…' : `Confirm Payment of ₹${Number(activePaymentModal.amount || 0).toFixed(2)}`}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
