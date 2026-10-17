import { useEffect, useState } from 'react'
import { apiRequest } from '../services/api'
export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [status, setStatus] = useState('Connecting to backend…')
  useEffect(() => {
    apiRequest('/admin/stats')
      .then(data => { setStats(data); setStatus('Live platform statistics loaded from API.') })
      .catch((err) => setStatus(err.message || 'Admin statistics requires an account with ADMIN role.'))
  }, [])
  return <section className="section page-section"><p className="eyebrow">ADMINISTRATION</p><h1>Admin dashboard</h1><p className="muted">{status}</p>
    <div className="dashboard-stats"><article><span className="stat-icon peach">♙</span><small>Users</small><strong>{stats?.users ?? '—'}</strong></article><article><span className="stat-icon mint">♡</span><small>Caregivers</small><strong>{stats?.caregivers ?? '—'}</strong></article><article><span className="stat-icon lavender">▤</span><small>Bookings</small><strong>{stats?.bookings ?? '—'}</strong></article></div>
    <div className="notice">Admin access allows viewing system metrics, verifying caregiver registrations, and managing all platform bookings.</div>
  </section>
}
