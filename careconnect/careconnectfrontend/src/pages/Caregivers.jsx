import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import CaregiverCard from '../components/CaregiverCard'
import { apiRequest } from '../services/api'

const demoCaregivers = [
  { id: 1, name: 'Aarohi Sharma', careType: 'Baby Care', experience: 4, available: true },
  { id: 2, name: 'Meera Patil', careType: 'Elder Care', experience: 6, available: true },
  { id: 3, name: 'Ananya Deshmukh', careType: 'Family Care', experience: 3, available: true }
]

export default function Caregivers() {
  const [caregivers, setCaregivers] = useState(demoCaregivers)
  const [notice, setNotice] = useState('Showing sample profiles. Connect the backend to load database records.')
  const [filter, setFilter] = useState('ALL')
  const navigate = useNavigate()

  useEffect(() => {
    let active = true
    apiRequest('/caregivers')
      .then(data => { if (active && Array.isArray(data)) { setCaregivers(data); setNotice('Caregivers loaded from the backend.') } })
      .catch(() => { if (active) setNotice('Backend not connected yet — these are sample profiles, not database records.') })
    return () => { active = false }
  }, [])

  const filtered = caregivers.filter(c => filter === 'ALL' || String(c.careType || c.care_type || '').toUpperCase().includes(filter))
  return <section className="section page-section"><div className="section-heading"><p className="eyebrow">MEET THE CARE TEAM</p><h1>Find a caregiver who fits your family.</h1><p>Explore sample profiles or connect the Spring Boot API to show real records.</p></div>
    <div className="filter-row"><label>Care type <select value={filter} onChange={e => setFilter(e.target.value)}><option value="ALL">All care types</option><option value="BABY">Baby care</option><option value="ELDER">Elder care</option></select></label><span className="muted">{filtered.length} profiles</span></div>
    <div className="notice">{notice}</div><div className="caregiver-grid">{filtered.map(c => <CaregiverCard key={c.id || c.name} caregiver={c} onBook={caregiver => navigate(`/booking?caregiverId=${caregiver.id}&type=${encodeURIComponent(caregiver.careType || caregiver.care_type || '')}`)} />)}</div>
  </section>
}
