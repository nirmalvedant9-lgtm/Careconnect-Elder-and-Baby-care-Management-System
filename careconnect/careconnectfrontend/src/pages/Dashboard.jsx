import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import { useAuth } from '../context/AuthContext'
import { apiRequest } from '../services/api'

export default function Dashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState({ bookings: 0, reminders: 0, caregivers: 0 })
  const [recentBookings, setRecentBookings] = useState([])

  useEffect(() => {
    apiRequest('/bookings')
      .then(b => {
        if (Array.isArray(b)) {
          setStats(s => ({ ...s, bookings: b.length }))
          setRecentBookings(b.slice(0, 3))
        }
      })
      .catch(() => {})

    apiRequest('/reminders')
      .then(r => { if (Array.isArray(r)) setStats(s => ({ ...s, reminders: r.length })) })
      .catch(() => {})

    apiRequest('/caregivers')
      .then(c => { if (Array.isArray(c)) setStats(s => ({ ...s, caregivers: c.length })) })
      .catch(() => {})
  }, [])

  return <div className="dashboard-layout"><Sidebar /><section className="dashboard-content">
    <p className="eyebrow">YOUR OVERVIEW</p><h1>Hello, {user?.name || 'there'} <span className="wave">✦</span></h1><p className="muted">Here is your CareConnect overview.</p>
    <div className="dashboard-stats">
      <article><span className="stat-icon peach">♡</span><small>Care requests</small><strong>{stats.bookings}</strong><span className="muted">Active bookings</span></article>
      <article><span className="stat-icon mint">♧</span><small>Caregivers</small><strong>{stats.caregivers}</strong><span className="muted">Available in network</span></article>
      <article><span className="stat-icon lavender">◷</span><small>Reminders</small><strong>{stats.reminders}</strong><span className="muted">Scheduled tasks</span></article>
    </div>

    {recentBookings.length > 0 && (
      <div className="dashboard-panel" style={{ marginBottom: '1.5rem' }}>
        <div><p className="eyebrow">RECENT REQUESTS</p><h2>Your Bookings</h2></div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '1rem' }}>
          {recentBookings.map(b => (
            <div key={b.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.85rem 1.2rem', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
              <div>
                <strong>Caregiver: {b.caregiver_name || `ID #${b.caregiver_id}`}</strong>
                <div style={{ fontSize: '0.85rem', color: '#64748b' }}>Date: {new Date(b.service_date).toLocaleDateString()} • {b.hours} hrs • For {b.dependent_name || 'Dependent'}</div>
              </div>
              <span className={`pill ${b.status === 'ACCEPTED' ? 'pill-green' : ''}`}>{b.status || 'PENDING'}</span>
            </div>
          ))}
        </div>
      </div>
    )}

    <div className="dashboard-panel"><div><p className="eyebrow">QUICK ACTIONS</p><h2>What would you like to do?</h2><p className="muted">Choose a service to continue.</p></div><div className="quick-actions"><Link to="/baby-care" className="quick-action"><span>🍼</span><strong>Explore baby care</strong><small>Support for little ones →</small></Link><Link to="/elder-care" className="quick-action"><span>🌿</span><strong>Explore elder care</strong><small>Support for older adults →</small></Link><Link to="/booking" className="quick-action"><span>＋</span><strong>Make a booking</strong><small>Request a caregiver →</small></Link></div></div>
  </section></div>
}
